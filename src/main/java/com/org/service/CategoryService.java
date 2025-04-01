package com.org.service;


import com.org.models.Category;

import java.util.List;

public interface CategoryService {
    List<Category> getAllCategories();
    Category getCategoryById(Long id);
    Category addCategory(Category category);
    Category updateCategory(Long id, Category updatedCategory);
    void deleteCategory(Long id);
}
