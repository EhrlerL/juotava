package com.juotava.recipes.service;

import com.juotava.recipes.model.*;
import com.juotava.recipes.model.Image;
import com.juotava.recipes.model.dto.imagegen.ImageRequest;
import com.juotava.recipes.model.dto.imagegen.ImageResponseSuccess;
import com.juotava.recipes.model.dto.textgen.Message;
import com.juotava.recipes.model.dto.textgen.TextRequest;
import com.juotava.recipes.model.dto.textgen.TextResponseSuccess;
import com.juotava.recipes.model.dto.weather.WeatherResponseSuccess;
import com.juotava.recipes.repository.bartinderFilter.BartinderFilterRepository;
import com.juotava.recipes.repository.bartinderFilter.SpringBartinderFilterRepository;
import com.juotava.recipes.repository.bartinderSuggestion.BartinderSuggestionRepository;
import com.juotava.recipes.repository.drinkOfTheDay.DrinkOfTheDayRepository;
import com.juotava.recipes.repository.filter.FilterRepository;
import com.juotava.recipes.repository.image.ImageRepository;
import com.juotava.recipes.repository.ingredient.IngredientRepository;
import com.juotava.recipes.repository.recipe.RecipeRepository;
import com.juotava.recipes.repository.recipeList.RecipeListRepository;
import com.juotava.recipes.repository.step.StepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Array;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RecipesService {

    @Qualifier("openaiRestTemplate")
    @Autowired
    private RestTemplate openaiRestTemplate;
    @Autowired
    private RestTemplate weatherApiRestTemplate;

    @Value("${openai.model.images}")
    private String imageModel;
    @Value("${openai.api.url.images}")
    private String imageApiUrl;
    @Value("${openai.model.text}")
    private String textModel;
    @Value("${openai.api.url.text}")
    private String textApiUrl;
    @Value("${weatherapi.api.url}")
    private String weatherApiUrl;
    @Value("${weatherapi.api.location}")
    private String weatherApiLocation;



    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final StepRepository stepRepository;
    private final ImageRepository imageRepository;
    private final RecipeListRepository recipeListRepository;
    private final FilterRepository filterRepository;
    private final DrinkOfTheDayRepository drinkOfTheDayRepository;
    private final BartinderFilterRepository bartinderFilterRepository;
    private final BartinderSuggestionRepository bartinderSuggestionRepository;
    private final SpringBartinderFilterRepository springBartinderFilterRepository;

    @Autowired
    public RecipesService(RecipeRepository recipeRepository, IngredientRepository ingredientRepository, StepRepository stepRepository, ImageRepository imageRepository, RecipeListRepository recipeListRepository, FilterRepository filterRepository, DrinkOfTheDayRepository drinkOfTheDayRepository, BartinderFilterRepository bartinderFilterRepository, BartinderSuggestionRepository bartinderSuggestionRepository,
                          SpringBartinderFilterRepository springBartinderFilterRepository) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.stepRepository = stepRepository;
        this.imageRepository = imageRepository;
        this.recipeListRepository = recipeListRepository;
        this.filterRepository = filterRepository;
        this.drinkOfTheDayRepository = drinkOfTheDayRepository;
        this.bartinderFilterRepository = bartinderFilterRepository;
        this.bartinderSuggestionRepository = bartinderSuggestionRepository;
        this.springBartinderFilterRepository = springBartinderFilterRepository;
    }

    //
    //  GETTERS
    //

    public Recipe getRecipe(UUID uuid, String auth0id){
        try {
            Recipe recipe = this.recipeRepository.findByUuid(uuid);
            if (recipe.isDraft() && !recipe.getCreatedBy().equals(auth0id)){
                System.out.println("ERROR: Recipe " + recipe.getUuid() + " is a draft and not owned by user "+ auth0id);
                return null;
            } else {
                return recipe;
            }
        } catch (Exception e){
            System.out.println("ERROR: Could not find recipe with UUID: " + uuid);
            return null;
        }
    }

    public List<Recipe> getAllRecipes(){
        return this.recipeRepository.findAllPublished();
    }

    public List<Recipe> getRecipesByUser(String auth0id){
        return this.recipeRepository.findByCreatedByAuth0id(auth0id);
    }

    public List<Recipe> getDraftedRecipesByUser(String auth0id){
        return this.recipeRepository.findDraftedByCreatedByAuth0id(auth0id);
    }

    public List<Recipe> getPublishedRecipesByUser(String auth0id){
        return this.recipeRepository.findPublishedByCreatedByAuth0id(auth0id);
    }

    public Map<String, Object> getAllRecipeExcerpts(String auth0id, String search, Integer page, Integer size) {
        // Get User's Filter
        Filter filter = getFilterByUser(auth0id, false);

        // Get User's Favorites
        RecipeList favorites = this.recipeListRepository.getFavoritesList(auth0id);

        // Create Pageable object
        Pageable pageable = PageRequest.of(page, size);

        // Split Search String
        List<String> searchInputs = new ArrayList<>();
        if (search != null) {
            if(search.contains(" ")) {
                searchInputs = List.of(search.split(" "));
            } else {
                searchInputs.add(search);
            }
        }
        // Generate Search Regex
        String searchRegex = searchInputs.stream()
            .map(Pattern::quote)
            .collect(Collectors.joining("|"));

        // Find published Recipes, paginate and search even when search is empty
        Page<Recipe> recipePage = recipeRepository.findAllPublishedSearchedAndFiltered(filter, searchRegex, pageable);

        // stream to apply filters and parse to excerpts
        List<RecipeExcerpt> excerpts = recipePage.stream()
            // parse Recipes to RecipeExcerpts and check for favorite
            .map(recipe -> this.parseToExcerpt(recipe, favorites))
            .toList();

        // create map
        Map<String, Object> response = new HashMap<>();
        response.put("excerpts", excerpts);
        response.put("currentPage", recipePage.getNumber());
        response.put("totalItems", recipePage.getTotalElements());
        response.put("totalPages", recipePage.getTotalPages());

        return response;
    }

    public List<RecipeExcerpt> searchInRecipeExcerpts(List<RecipeExcerpt> excerpts, String search) {
        List<RecipeExcerpt> tempExcerpts = new ArrayList<>();
        for (RecipeExcerpt excerpt : excerpts) {
            if(excerpt.find(search.toLowerCase())){
                tempExcerpts.add(excerpt);
            }
        }
        tempExcerpts.sort((r1, r2) ->
            r1.getPrio()<r2.getPrio() ? 1 :
                r1.getPrio()>r2.getPrio() ? -1 :
                    0
        );
        return tempExcerpts;
    }

    public List<RecipeExcerpt> getDraftedRecipeExcerptsByUser(String auth0id){
        RecipeList favorites = this.recipeListRepository.getFavoritesList(auth0id);
        return getDraftedRecipesByUser(auth0id).stream()
                .map(recipe -> parseToExcerpt(recipe, favorites))
                .collect(Collectors.toList());
    }

    public List<RecipeExcerpt> getPublishedRecipeExcerptsByUser(String auth0id){
        RecipeList favorites = this.recipeListRepository.getFavoritesList(auth0id);
        return getPublishedRecipesByUser(auth0id).stream()
                .map(recipe -> parseToExcerpt(recipe, favorites))
                .collect(Collectors.toList());
    }

    //
    // SETTERS
    //

    public boolean saveRecipe(Recipe recipe){
        try {
            Recipe existing = this.recipeRepository.findByUuid(recipe.getUuid());
            if (!recipe.getCreatedBy().equals(existing.getCreatedBy())){
                System.out.println("ERROR: Recipe "+ recipe.getUuid()+" exists but does not belong to user "+recipe.getCreatedBy());
                return false;
            }
            throw new Exception("");
        } catch (Exception e){
            recipe.getIngredients().forEach(this.ingredientRepository::save);
            recipe.getSteps().forEach(this.stepRepository::save);
            this.imageRepository.save(recipe.getImage());
            this.recipeRepository.save(recipe);
            System.out.println("INFO: Saved Recipe "+ recipe.getUuid());
            return true;
        }


    }

    public void addIngredientToRecipe(Recipe recipe, Ingredient ingredient){
        this.ingredientRepository.save(ingredient);
        recipe.addIngredient(ingredient);
        this.recipeRepository.save(recipe);
    }

    public void addStepToRecipe(Recipe recipe, Step step){
        this.stepRepository.save(step);
        recipe.addStep(step);
        this.recipeRepository.save(recipe);
    }

    public void setImageOfRecipe(Recipe recipe, Image image){
        this.imageRepository.save(image);
        recipe.setImage(image);
        this.recipeRepository.save(recipe);
    }

    public void setCreatedByOfRecipe(Recipe recipe, String auth0id){
        recipe.setCreatedBy(auth0id);
        this.recipeRepository.save(recipe);
    }

    public void setCurrentUserToRecipe(Recipe recipe, String auth0id){
        recipe.setCreatedBy(auth0id);
    }

    public List<RecipeListExcerpt> getRecipeListsByUser(String auth0id) {
        if(this.recipeListRepository.getFavoritesList(auth0id) == null){
            RecipeList favoriteList = new RecipeList("Favoriten", auth0id);
            this.recipeListRepository.save(favoriteList);
            System.out.println("Info: Creating new Favorites List");
        }
        List<RecipeList> recipeLists = this.recipeListRepository.findByCreatedByAuth0id(auth0id);
        List<RecipeListExcerpt> recipeListExcerpts = recipeLists.stream().map((RecipeListExcerpt::new)).toList();
        return recipeListExcerpts;
    }

    public String createRecipeList(String title, String auth0id) {
        try {
            RecipeList recipeList = new RecipeList(title, auth0id);
            this.recipeListRepository.save(recipeList);
            return recipeList.getUuid().toString();
        } catch (Exception e) {
            System.out.println("Error: List title is too long and could not be saved");
            return "false";
        }
    }

    public boolean addRecipeToList(UUID listId, UUID recipeId, String auth0id) {
        try {
            RecipeList list = this.recipeListRepository.findByUuid(listId);
            if (!auth0id.equals(list.getCreatedBy())){
                System.out.println("Error: List creator id (" + list.getCreatedBy().toString() + ") does not match auth0id (" + auth0id + ")");
                return false;
            }
            if (list.getRecipes().stream().anyMatch(recipe -> recipe.getUuid().equals(recipeId))){
                System.out.println("Error: Recipe already in list");
                return false;
            }
            list.addRecipeToList(this.recipeRepository.findByUuid(recipeId));
            this.recipeListRepository.save(list);
            return true;
        } catch (Exception e){
            System.out.println("Error: Recipe does not exist");
            return false;
        }
    }

    public boolean addRecipeToFavorite(UUID recipeId, String auth0id) {
        try {
            RecipeList favoriteList = this.recipeListRepository.getFavoritesList(auth0id);
            favoriteList.addRecipeToList(this.recipeRepository.findByUuid(recipeId));
            this.recipeListRepository.save(favoriteList);
            return true;
        } catch (Exception e){
            try {
                RecipeList favoriteList = new RecipeList("Favoriten", auth0id);
                favoriteList.addRecipeToList(this.recipeRepository.findByUuid(recipeId));
                this.recipeListRepository.save(favoriteList);
                System.out.println("Info: Creating new Favorites List");
                return true;
            } catch (Exception x){
                System.out.println("Error: Recipe does not exist");
                return false;
            }

        }
    }

    public boolean removeRecipeFromFavorites(UUID recipeId, String auth0id){
        RecipeList favoriteList = this.recipeListRepository.getFavoritesList(auth0id);
        return this.removeRecipeFromList(favoriteList.getUuid(), recipeId, auth0id);
    }

    public boolean deleteRecipe(UUID recipeId, String auth0id) {
        try {
            Recipe recipe = this.recipeRepository.findByUuid(recipeId);
            if (!auth0id.equals(recipe.getCreatedBy())){
                System.out.println("ERROR: Recipe "+ recipe.getUuid()+" exists but does not belong to user "+recipe.getCreatedBy());
                return false;
            }
            List<RecipeList> recipeLists = this.recipeListRepository.findByRecipeId(recipeId);
            recipeLists.forEach(list -> list.removeRecipeFromList(recipe));
            this.recipeRepository.delete(recipe);
            return true;
        } catch (Exception e){
            System.out.println("ERROR: Could not find recipe with UUID: " + recipeId);
            return false;
        }
    }

    public RecipeExcerptsList getRecipeList(UUID listId, String auth0id) {
        try {
            RecipeList originalList = this.recipeListRepository.findByUuid(listId);
            RecipeList favoriteList = this.recipeListRepository.getFavoritesList(auth0id);
            if (!auth0id.equals(originalList.getCreatedBy()) || originalList.getRecipes() == null){
                return null;
            }
            List<RecipeExcerpt> excerpts = originalList.getRecipes().stream()
                    .map(recipe -> parseToExcerpt(recipe, favoriteList))
                    .toList();
            return new RecipeExcerptsList(originalList, excerpts);
        } catch (Exception e){
            System.out.println("Warning: List does not exist" + listId);
            return null;
        }
    }

    public boolean removeRecipeFromList(UUID listId, UUID recipeId, String auth0id) {
        try {
            RecipeList list = this.recipeListRepository.findByUuid(listId);
            if (!auth0id.equals(list.getCreatedBy())){
                return false;
            }
            list.removeRecipeFromList(this.recipeRepository.findByUuid(recipeId));
            this.recipeListRepository.save(list);
            return true;
        } catch (Exception e){
            System.out.println("Warning: List does not exist" + listId);
            return false;
        }
    }

    public boolean isRecipeInFavorites(UUID listId, String auth0id) {
        RecipeList favoriteList = this.recipeListRepository.getFavoritesList(auth0id);
        return favoriteList != null? 
            favoriteList.getRecipes().stream().anyMatch(recipe -> recipe.getUuid().equals(listId))
            : false;
    }

    //
    //  GENERATION
    //

    public Image generateImage(String prompt, String user){
        ImageRequest request = new ImageRequest(prompt, imageModel, 1, "hd", "b64_json", user);
        ImageResponseSuccess response = openaiRestTemplate.postForObject(imageApiUrl, request, ImageResponseSuccess.class);

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return null;
        }
        byte[] imageBytes = Base64.getDecoder().decode(response.getData().get(0).getB64_json());
        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage;
        try {
            originalImage = ImageIO.read(bis);
        } catch (IOException e) {
            System.out.println("ERROR: Generated Image could not be converted to bytestream");
            return null;
        }

        BufferedImage resizedImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, 512, 512, null);
        g2d.dispose();
        String resizedImageBase64 = encodeImageToBase64(resizedImage);

        return new Image(
                response.getData().get(0).getRevised_prompt(),
                "data:image/png;base64,"+resizedImageBase64
        );
    }

    private String encodeImageToBase64(BufferedImage image) {
        // Convert BufferedImage to byte[]
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", bos);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // Encode byte[] to base64 string
        byte[] imageBytes = bos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public DrinkOfTheDay getDrinkOfTheDay(){
        try {
            DrinkOfTheDay drinkOfTheDay = this.drinkOfTheDayRepository.findByDateToday();
            if (drinkOfTheDay != null){
                return drinkOfTheDay;
            } else {
                throw new Exception();
            }
        } catch (Exception e){
            System.out.println("INFO: No drink of the day for today, creating new ...");
            List<Recipe> recipes = this.recipeRepository.findAllPublished();
            if (!recipes.isEmpty()){
                DrinkOfTheDay drinkOfTheDay = this.generateDrinkOfTheDay(LocalDate.now(), recipes);
                if (drinkOfTheDay != null){
                    this.drinkOfTheDayRepository.save(drinkOfTheDay);
                }
                return drinkOfTheDay;
            } else {
                System.out.println("ERROR: There are no recipes from which a drink of the day could be chosen");
                return null;
            }
        }

    }

    public DrinkOfTheDay generateDrinkOfTheDay(LocalDate date, List<Recipe> recipes){
        String weather = "";
        if (date.equals(LocalDate.now())){
            weather = requestTodaysWeather();
        }

        String recipesString = recipes.stream().map(Recipe::toRecipeOfTheDayString).collect(Collectors.joining(","));
        TextRequest request = new TextRequest(textModel);
        request.addMessage(new Message("system", "You are a deciding machine. I will give you a list of Recipes in the format <ID: XXX, Title: YYY, Description: ZZZ>, a date and some weather data. Based on these information you must decide on which Recipe you choose as the drink of the day. Keep in mind that some drinks are better fitted in certain seasons or temperatures. Consider Holidays or special Events. Give a sound reasoning text in German. Use the colloquial 'Du'. Include some Trivia in the reasoning. Do not mention exact numbers on weather data in the reasoning. Your answer must under all circumstances follow: <ID: XXX, REASON: YYY>. Do not write any additional text!"));
        request.addMessage(new Message("user",
                "Today is "+date.toString() +" " +
                        weather + " " +
                        "The recipes are: ( "+ recipesString +")"
        ));

        for (int i = 0; i < 3; i++) {
            try {
                TextResponseSuccess response = openaiRestTemplate.postForObject(textApiUrl, request, TextResponseSuccess.class);
                if (response == null || response.getChoices() == null || response.getChoices().isEmpty()){
                    return null;
                }
                DrinkOfTheDay drinkOfTheDay = new DrinkOfTheDay(
                        date, response.getChoices().get(0).getMessage().getContent()
                );
                drinkOfTheDay.setRecipe(
                        this.recipeRepository.findByUuid(drinkOfTheDay.getParsedRecipeUuid())
                );
                return  drinkOfTheDay;
            } catch (Exception e){
                System.out.println("ERROR: Attempt "+(i+1)+". A drink of the day could not be generated. This is most likely due to GPT giving a non-conforming answer.");
            }
        }
        return null;
    }

    public String requestTodaysWeather(){
        try {
            WeatherResponseSuccess response = weatherApiRestTemplate.getForObject(weatherApiUrl, WeatherResponseSuccess.class);
            if (response == null || response.getForecast().getForecastday().get(0).getDay() == null){
                return null;
            }
            return response.getForecast().getForecastday().get(0).getDay().toString();
        } catch (Exception e){
            return null;
        }
    }

    //
    //  FILTERS
    //

    public boolean saveFilter(Filter filter, String auth0id) {
        if (!auth0id.equals(filter.getCorrespondingUser())){
            System.out.println("ERROR: Filter correspondingUser "+filter.getCorrespondingUser()+" does not match with sending user "+ auth0id);
            return false;
        }
        try {
            Filter existing = this.filterRepository.findByUuid(filter.getUuid());
            if (!existing.getCorrespondingUser().equals(filter.getCorrespondingUser())) {
                System.out.println("ERROR: Filter " + filter.getUuid() + " exists but does not belong to user " + auth0id);
                return false;
            } else {
                throw new Exception();
            }
        } catch (Exception e){
            this.filterRepository.save(filter);
            System.out.println("INFO: Saved filter "+ filter.getUuid());
            return true;
        }


    }

    public Filter getFilterByUser(String auth0id, boolean createNew) {

        try {

            Filter filter = filterRepository.findByCorrespondingUserAuth0id(auth0id);
            if (filter == null) {
                throw new Exception("Cannot find Filter");
            }
            return filter;
        } catch (Exception e) {
            Filter filter = new Filter();
            filter.setCorrespondingUser(auth0id);
            if (createNew) { this.filterRepository.save(filter); }
            return filter;
        }
    }

    public RecipeExcerpt parseToExcerpt(Recipe recipe, RecipeList favorites) {
        return new RecipeExcerpt(recipe.getUuid(), recipe.getTitle(), recipe.getCategory(), recipe.isNonAlcoholic(), recipe.getDescription(), recipe.getIngredients(), recipe.getImage(),
            favorites != null? favorites.getRecipes().stream().anyMatch(f -> f.getUuid().equals(recipe.getUuid())) : false
        );
    }

    public boolean changeRecipeList(UUID listId, String newTitle, String auth0id) {
        try {
            RecipeList recipeList = this.recipeListRepository.findByUuid(listId);
            if (!auth0id.equals(recipeList.getCreatedBy()) || recipeList.getTitle().equals("Favoriten")){
                return false;
            }
            recipeList.setTitle(newTitle);
            this.recipeListRepository.save(recipeList);
            return true;
        } catch (Exception e){
            System.out.println("Warning: List does not exist" + listId);
            return false;
        }
    }

    //
    // Bartinder
    //

    public Recipe getNextBartind(String auth0id){
        BartinderFilter filter = this.bartinderFilterRepository.findByCorrespondingUserAuth0id(auth0id);
        List<BartinderSuggestion> suggestions = this.bartinderSuggestionRepository.findByCorrespondingUser(auth0id);
        if (filter == null){
            return null;
        }
        return this.recipeRepository.findRandomByBartinderFilter(filter, suggestions);
    }

    public BartinderFilter getBartinderFilter(String auth0id){
        return this.bartinderFilterRepository.findByCorrespondingUserAuth0id(auth0id);
    }

    public boolean saveBartinderFilter(BartinderFilter filter, String auth0id) {
        if (!auth0id.equals(filter.getCorrespondingUser())){
            System.out.println("ERROR: Filter correspondingUser "+filter.getCorrespondingUser()+" does not match with sending user "+ auth0id);
            return false;
        }
        try {
            BartinderFilter existing = this.bartinderFilterRepository.findByCorrespondingUserAuth0id(auth0id);
            if (existing == null){
                System.out.println("INFO: Saving new BartinderFilter for user "+auth0id);
                this.springBartinderFilterRepository.save(filter);
                return true;
            } else if (!existing.getCorrespondingUser().equals(filter.getCorrespondingUser())) {
                System.out.println("ERROR: Filter " + filter.getUuid() + " exists but does not belong to user " + auth0id);
                return false;
            } else {
                existing.setCategories(filter.getCategories());
                existing.setShowNonAlcOnly(filter.isShowNonAlcOnly());
                System.out.println("INFO: Updating existing BartinderFilter for user "+auth0id);
                this.springBartinderFilterRepository.save(existing);
                return true;
            }
        } catch (Exception e){
            System.out.println("ERROR: Could not save BartinderFilter "+ filter.getUuid());
            return false;
        }
    }

    public boolean saveBartinderSuggestion(BartinderSuggestion suggestion, String auth0id) {
        try {
            suggestion.setCorrespondingUser(auth0id);
            this.bartinderSuggestionRepository.save(suggestion);
            if (suggestion.isLiked()){
                this.addRecipeToFavorite(suggestion.getRecipeUuid(), auth0id);
            }
            return true;
        } catch (Exception e){
            System.out.println("ERROR: Could not save BartinderSuggestion");
            return false;
        }
    }
}
