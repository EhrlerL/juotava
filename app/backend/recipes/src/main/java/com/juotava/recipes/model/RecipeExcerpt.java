package com.juotava.recipes.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Getter
public class RecipeExcerpt {
    @Id
    @GeneratedValue
    private UUID uuid;

    private String title;

    private String category;

    private boolean nonAlcoholic;

    private String description;

    @OneToMany
    private List<Ingredient> ingredients;

    @OneToOne
    private Image image;

    private Integer prio;

    @Setter
    private boolean favorite;

    public RecipeExcerpt(UUID uuid, String title, String category, boolean nonAlcoholic, String description, List<Ingredient> ingredients, Image image, boolean favorite) {
        this.uuid = uuid;
        this.title = title;
        this.category = category;
        this.nonAlcoholic = nonAlcoholic;
        this.description = description;
        this.ingredients = ingredients;
        this.image = image;
        this.favorite = favorite;
    }

    public RecipeExcerpt() {
        this.title = "";
        this.description = "";
        this.ingredients = new ArrayList<>();
    }

    public List<Ingredient> getIngredients() {
        this.ingredients.sort(Comparator.comparingInt(Ingredient::getOrder));
        return ingredients;
    }

    public String toString() {
        String text = "";
        text += this.title+"; ";
        text += this.category+"; ";
        text += this.description+"; ";
        for (Ingredient ingredient : this.ingredients) {
            text += ingredient.getName()+", ";
        }
        return text.toLowerCase();
    }

    public boolean find(String search){
        List<String> decodedQuery = List.of(search.split(" "));
        this.prio = 0;
        for (String string : decodedQuery) {
            if (this.toString().contains(string)) {
                this.prio++;
            }
        }
        return prio > 0;
    }
}