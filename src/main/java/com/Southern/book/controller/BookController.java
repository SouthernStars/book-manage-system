package com.Southern.book.controller;

import com.Southern.book.entity.Book;
import com.Southern.book.entity.Category;
import com.Southern.book.service.BookService;
import com.Southern.book.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;

@Controller
@RequestMapping("/books")
public class BookController {
    @Autowired
    private BookService bookService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Value("${file.upload-dir}")
    private String UPLOAD_DIR;

    // 分页显示图书列表和处理搜索
    @GetMapping
    public String viewBookList(Model model, 
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) Long categoryId,
                             @RequestParam(required = false) String status) {
        // 处理搜索
        List<Book> books;
        if (keyword != null || categoryId != null || status != null) {
            books = bookService.searchBooks(keyword, categoryId, status);
        } else {
            books = bookService.getAllBooks();
        }
        
        // 设置分页相关属性
        model.addAttribute("books", books);
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        
        // 设置总记录数和分页信息
        int totalElements = books.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startIndex", startIndex);
        model.addAttribute("endIndex", endIndex);
        
        // 生成页码数组
        List<Integer> pageNumbers = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageNumbers.add(i);
        }
        model.addAttribute("pageNumbers", pageNumbers);
        
        // 添加分类列表供搜索表单使用
        model.addAttribute("categories", categoryService.getAllCategories());
        
        return "books/list";
    }

    // 显示添加图书表单
    @GetMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAddForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "books/add";
    }

    // 添加图书
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String addBook(@ModelAttribute Book book, @RequestParam("coverImageFile") MultipartFile file,
                         @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds) {
        // 处理图片上传
        if (!file.isEmpty()) {
            try {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                // 确保上传路径正确，包含末尾的斜杠
                String uploadPath = UPLOAD_DIR.endsWith("/") ? UPLOAD_DIR : UPLOAD_DIR + "/";
                Path path = Paths.get(uploadPath + fileName);
                Files.createDirectories(path.getParent());
                Files.write(path, file.getBytes());
                book.setCoverImage("/images/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // 添加分类
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Long categoryId : categoryIds) {
                categoryService.getCategoryById(categoryId).ifPresent(book::addCategory);
            }
        }
        
        bookService.addBook(book);
        return "redirect:/books";
    }

    // 显示编辑图书表单
    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model) {
        return bookService.getBookById(id)
                .map(book -> {
                    model.addAttribute("book", book);
                    model.addAttribute("categories", categoryService.getAllCategories());
                    return "books/edit";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "找不到指定的图书");
                    return "redirect:/books";
                });
    }

    // 更新图书
    @PostMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateBook(@PathVariable Long id, @ModelAttribute Book book, 
                           @RequestParam(value = "coverImageFile", required = false) MultipartFile file,
                           @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds) {
        // 先获取现有图书信息，保留原有的封面图片
        Optional<Book> existingBook = bookService.getBookById(id);
        if (existingBook.isPresent()) {
            // 只有在用户上传新封面图片时才更新coverImage字段
            if (file != null && !file.isEmpty()) {
                try {
                    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                    // 确保上传路径正确，包含末尾的斜杠
                    String uploadPath = UPLOAD_DIR.endsWith("/") ? UPLOAD_DIR : UPLOAD_DIR + "/";
                    Path path = Paths.get(uploadPath + fileName);
                    Files.createDirectories(path.getParent());
                    Files.write(path, file.getBytes());
                    book.setCoverImage("/images/" + fileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // 如果用户没有上传新封面，则保留原有的封面图片
                book.setCoverImage(existingBook.get().getCoverImage());
            }
        }
        
        // 添加分类
        book.getCategories().clear();
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Long categoryId : categoryIds) {
                categoryService.getCategoryById(categoryId).ifPresent(book::addCategory);
            }
        }
        
        bookService.updateBook(id, book);
        return "redirect:/books";
    }

    // 查看图书详情
    @GetMapping("/view/{id}")
    public String viewBook(@PathVariable Long id, Model model) {
        return bookService.getBookById(id)
                .map(book -> {
                    model.addAttribute("book", book);
                    return "books/view";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "找不到指定的图书");
                    return "redirect:/books";
                });
    }
    
    // 删除图书
    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return "redirect:/books";
    }

    // 搜索图书
    @GetMapping("/search")
    public String searchBooks(@RequestParam String keyword, Model model) {
        List<Book> books = bookService.searchBooks(keyword);
        model.addAttribute("books", books);
        model.addAttribute("keyword", keyword);
        return "books/list";
    }
}