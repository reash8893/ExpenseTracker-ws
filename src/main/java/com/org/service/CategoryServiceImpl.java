package com.org.service;

import com.org.models.Category;
import com.org.entities.CategoryEntity;
import com.org.repositories.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public List<Category> getAllCategories() {
        List<CategoryEntity> entities = categoryRepository.findAll();
        return entities.stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(this::convertToModel)
                .orElse(null);
    }

    @Override
    @Transactional
    public Category addCategory(Category category) {
        CategoryEntity entity = convertToEntity(category);
        CategoryEntity savedEntity = categoryRepository.save(entity);
        return convertToModel(savedEntity);
    }

    @Override
    @Transactional
    public Category updateCategory(Long id, Category updatedCategory) {
        return categoryRepository.findById(id)
                .map(existingEntity -> {
                    existingEntity.setDescription(updatedCategory.getCategoryName());
                    CategoryEntity savedEntity = categoryRepository.save(existingEntity);
                    return convertToModel(savedEntity);
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    private Category convertToModel(CategoryEntity entity) {
        Category category = new Category();
        category.setCategoryId(entity.getCategoryId());
        category.setCategoryName(entity.getDescription());
        return category;
    }

    private CategoryEntity convertToEntity(Category category) {
        CategoryEntity entity = new CategoryEntity();
        entity.setCategoryId(category.getCategoryId());
        entity.setDescription(category.getCategoryName());
        return entity;
    }
}
