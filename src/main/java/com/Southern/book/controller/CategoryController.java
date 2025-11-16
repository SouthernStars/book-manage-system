package com.Southern.book.controller;

import com.Southern.book.entity.Category;
import com.Southern.book.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public String listCategories(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        return "admin/category/list";
    }

    @GetMapping("/add")
    public String showAddForm() {
        // 由于使用弹窗添加，直接重定向到列表页面
        return "redirect:/admin/categories";
    }

    @PostMapping("/add")
    public String addCategory(@ModelAttribute Category category) {
        categoryService.addCategory(category);
        return "redirect:/admin/categories?success=add";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id) {
        // 由于使用弹窗编辑，直接重定向到列表页面
        return "redirect:/admin/categories";
    }

    @PostMapping("/edit/{id}")
    public String updateCategory(@PathVariable Long id, @ModelAttribute Category category) {
        categoryService.updateCategory(id, category);
        return "redirect:/admin/categories?success=edit";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return "redirect:/admin/categories?success=delete";
        } catch (Exception e) {
            // 处理删除异常（如分类下有图书）
            return "redirect:/admin/categories?error=hasBooks";
        }
    }
}