package com.juotava.recipes.model;

import com.juotava.recipes.model.enums.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
public class Ingredient {

    @Id
    @GeneratedValue
    private UUID uuid;
    @Column(name = "ingredientorder")
    private int order;
    private String name;
    private int amount;
    private Unit unit;

    public Ingredient(String name, int amount, Unit unit) {
        this.name = name;
        this.amount = amount;
        this.unit = unit;
    }

    public Ingredient() {
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
