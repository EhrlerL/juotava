package com.juotava.recipes.service;

import com.juotava.recipes.model.*;
import com.juotava.recipes.model.Image;
import com.juotava.recipes.model.dto.ImageRequest;
import com.juotava.recipes.model.dto.ImageResposeSuccess;
import com.juotava.recipes.repository.image.ImageRepository;
import com.juotava.recipes.repository.ingredient.IngredientRepository;
import com.juotava.recipes.repository.recipe.RecipeRepository;
import com.juotava.recipes.repository.recipeList.RecipeListRepository;
import com.juotava.recipes.repository.step.StepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecipesService {

    @Qualifier("openaiRestTemplate")
    @Autowired
    private RestTemplate openaiRestTemplate;
    @Value("${openai.model}")
    private String model;
    @Value("${openai.api.url}")
    private String apiUrl;


    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final StepRepository stepRepository;
    private final ImageRepository imageRepository;
    private final RecipeListRepository recipeListRepository;

    @Autowired
    public RecipesService(RecipeRepository recipeRepository, IngredientRepository ingredientRepository, StepRepository stepRepository, ImageRepository imageRepository, RecipeListRepository recipeListRepository) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.stepRepository = stepRepository;
        this.imageRepository = imageRepository;
        this.recipeListRepository = recipeListRepository;
    }

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

    public List<RecipeExcerpt> getAllRecipeExcerpts() {
        List<RecipeExcerpt> tempList = new ArrayList<>();
        getAllRecipes().stream()
                .map(elt -> tempList.add(new RecipeExcerpt(elt.getUuid(), elt.getTitle(), elt.getCategory(), elt.isNonAlcoholic(), elt.getDescription(), elt.getIngredients(), elt.getImage())))
                .collect(Collectors.toList());
        return(tempList);
    }

    public void saveRecipe(Recipe recipe){
        recipe.getIngredients().forEach(this.ingredientRepository::save);
        recipe.getSteps().forEach(this.stepRepository::save);
        this.imageRepository.save(recipe.getImage());
        this.recipeRepository.save(recipe);
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

    public List<RecipeList> getRecipeListsByUser(String auth0id) {
        return this.recipeListRepository.findByCreatedByAuth0id(auth0id);
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

    public RecipeList getRecipeList(UUID listId, String auth0id) {
        try {
            RecipeList list = this.recipeListRepository.findByUuid(listId);
            if (!auth0id.equals(list.getCreatedBy())){
                return null;
            }
            return list;
        } catch (Exception e){
            System.out.println("Warning: List does not exist" + listId);
            return null;
        }
    }

    public Image generateImage(String prompt, String user){
        ImageRequest request = new ImageRequest(prompt, model, 1, "hd", "b64_json", user);
        ImageResposeSuccess response = openaiRestTemplate.postForObject(apiUrl, request, ImageResposeSuccess.class);

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
}
