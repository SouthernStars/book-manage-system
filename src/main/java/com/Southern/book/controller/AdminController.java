package com.Southern.book.controller;

import com.Southern.book.service.BookService;
import com.Southern.book.service.UserService;
import com.Southern.book.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private BorrowService borrowService;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("totalBooks", bookService.getTotalBookCount());
        model.addAttribute("availableBooks", bookService.getAvailableBookCount());
        model.addAttribute("totalUsers", userService.getTotalUserCount());
        model.addAttribute("activeBorrows", borrowService.getActiveBorrowCount());
        model.addAttribute("overdueRecords", borrowService.getOverdueCount());
        return "admin/dashboard";
    }

    @GetMapping
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }
}