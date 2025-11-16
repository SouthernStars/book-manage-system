package com.Southern.book.service;

import com.Southern.book.entity.Category;
import com.Southern.book.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    // 获取所有分类
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // 根据ID查找分类
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    // 根据名称查找分类
    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    // 添加分类
    @Transactional
    public Category addCategory(Category category) {
        return categoryRepository.save(category);
    }

    // 更新分类
    @Transactional
    public Optional<Category> updateCategory(Long id, Category categoryDetails) {
        return categoryRepository.findById(id).map(category -> {
            category.setName(categoryDetails.getName());
            category.setDescription(categoryDetails.getDescription());
            return categoryRepository.save(category);
        });
    }

    // 删除分类
    @Transactional
    public boolean deleteCategory(Long id) {
        return categoryRepository.findById(id).map(category -> {
            // 检查分类是否有关联的图书
            if (category.getBooks() != null && !category.getBooks().isEmpty()) {
                throw new IllegalStateException("该分类下存在图书，无法删除");
            }
            categoryRepository.delete(category);
            return true;
        }).orElse(false);
    }

    // 检查分类名称是否已存在
    public boolean existsByName(String name) {
        return categoryRepository.findByName(name) != null;
    }
}