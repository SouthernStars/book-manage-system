package com.Southern.book.controller;

import com.Southern.book.entity.BorrowRecord;
import com.Southern.book.entity.User;
import com.Southern.book.service.BorrowService;
import com.Southern.book.service.BookService;
import com.Southern.book.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
@RequestMapping("/borrow")
public class BorrowRecordController {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @GetMapping({"/records", "/record"})
    @PreAuthorize("hasRole('ADMIN')")
    public String listBorrowRecords(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(required = false) String username,
                                   @RequestParam(required = false) String bookTitle,
                                   @RequestParam(required = false) String status,
                                   Model model) {
        Pageable pageable = PageRequest.of(page - 1, 10, Sort.by(Sort.Direction.DESC, "borrowDate"));
        List<BorrowRecord> records = borrowService.getAllBorrowRecords();
        
        // 模拟分页和搜索功能
        model.addAttribute("records", records);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); // 简化处理，实际应该根据总数计算
        model.addAttribute("username", username);
        model.addAttribute("bookTitle", bookTitle);
        model.addAttribute("status", status);
        
        // 添加用于借阅模态框的图书和用户数据
        model.addAttribute("books", bookService.getAvailableBooks());
        model.addAttribute("users", userService.getAllUsers());
        
        return "borrow/record";
    }

    @GetMapping("/my-records")
    @PreAuthorize("hasRole('USER')")
    public String listMyBorrowRecords(@RequestParam(defaultValue = "1") int page, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        List<BorrowRecord> records = borrowService.getBorrowRecordsByUsername(username);
        
        // 计算统计数据
        int currentBorrowCount = 0;
        int overdueCount = 0;
        int historyCount = 0;
        int totalBorrowDays = 0;
        
        for (BorrowRecord record : records) {
            if ("RETURNED".equals(record.getStatus())) {
                historyCount++;
                // 计算借阅天数
                if (record.getReturnDate() != null) {
                    totalBorrowDays += ChronoUnit.DAYS.between(record.getBorrowDate(), record.getReturnDate());
                }
            } else {
                currentBorrowCount++;
                if ("OVERDUE".equals(record.getStatus()) || LocalDate.now().isAfter(record.getDueDate())) {
                    overdueCount++;
                }
            }
        }
        
        model.addAttribute("records", records);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); // 简化处理
        model.addAttribute("currentBorrowCount", currentBorrowCount);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("historyCount", historyCount);
        model.addAttribute("totalBorrowDays", totalBorrowDays);
        
        return "borrow/my_record";
    }

    @GetMapping("/borrow/borrow")
    @PreAuthorize("hasRole('ADMIN')")
    public String showBorrowForm(Model model) {
        model.addAttribute("books", bookService.getAvailableBooks());
        model.addAttribute("users", userService.getAllUsers());
        
        // 添加近期借阅记录
        List<BorrowRecord> allRecords = borrowService.getAllBorrowRecords();
        List<BorrowRecord> recentRecords = allRecords.stream()
                .limit(5)
                .toList();
        model.addAttribute("recentRecords", recentRecords);
        
        return "borrow/borrow";
    }

    @PostMapping("/borrow/borrow")
    @PreAuthorize("hasRole('ADMIN')")
    public String borrowBook(@RequestParam Long userId, @RequestParam Long bookId, @RequestParam int days) {
        borrowService.borrowBook(userId, bookId, days);
        return "redirect:/borrow/records";
    }

    @GetMapping("/return/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String returnBook(@PathVariable Long id) {
        borrowService.returnBook(id).orElseThrow(() -> new IllegalStateException("借阅记录不存在"));
        return "redirect:/borrow/records";
    }
    
    @GetMapping("/user/borrow")
    @PreAuthorize("hasRole('USER')")
    public String showUserBorrowForm(Model model, @RequestParam(required = false) Long bookId) {
        // 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userService.getUserByUsername(username).orElseThrow(() -> new IllegalStateException("用户不存在"));
        model.addAttribute("currentUser", currentUser);
        
        // 添加可用图书列表
        model.addAttribute("books", bookService.getAvailableBooks());
        
        // 如果传入了bookId，则预填图书信息
        if (bookId != null) {
            bookService.getBookById(bookId).ifPresent(book -> model.addAttribute("selectedBook", book));
        }
        
        // 添加近期借阅记录
        List<BorrowRecord> recentRecords = borrowService.getBorrowRecordsByUsername(username).stream()
                .limit(5)
                .toList();
        model.addAttribute("recentRecords", recentRecords);
        
        return "borrow/borrow";
    }
    
    @PostMapping("/user/borrow")
    @PreAuthorize("hasRole('USER')")
    public String userBorrowBook(@RequestParam Long bookId, @RequestParam int days) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.getUserByUsername(username).orElseThrow(() -> new IllegalStateException("用户不存在"));
        
        try {
            borrowService.borrowBook(user.getId(), bookId, days);
            return "redirect:/borrow/my-records?success=true";
        } catch (Exception e) {
            return "redirect:/borrow/user/borrow?error=" + e.getMessage() + "&bookId=" + bookId;
        }
    }
    
    @GetMapping("/user/return/{id}")
    @PreAuthorize("hasRole('USER')")
    public String userReturnBook(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // Verify that the borrow record belongs to the current user
        BorrowRecord record = borrowService.getBorrowRecordById(id).orElseThrow(() -> new IllegalStateException("借阅记录不存在"));
        if (!record.getUser().getUsername().equals(username)) {
            throw new IllegalStateException("无权操作此借阅记录");
        }
        
        borrowService.returnBook(id);
        return "redirect:/borrow/my-records";
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public String listOverdueRecords(@RequestParam(defaultValue = "1") int page, Model model) {
        List<BorrowRecord> overdueRecords = borrowService.getOverdueRecords();
        
        // 计算逾期统计数据
        int totalOverdueDays = 0;
        double totalFine = 0;
        int mostOverdueDays = 0;
        Set<User> overdueUsers = new HashSet<>();
        
        for (BorrowRecord record : overdueRecords) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), LocalDate.now());
            totalOverdueDays += overdueDays;
            mostOverdueDays = Math.max(mostOverdueDays, (int) overdueDays);
            overdueUsers.add(record.getUser());
            
            if (record.getFineAmount() != null) {
                totalFine += record.getFineAmount();
            }
        }
        
        model.addAttribute("records", overdueRecords);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); // 简化处理
        model.addAttribute("totalOverdueDays", totalOverdueDays);
        model.addAttribute("totalFine", totalFine);
        model.addAttribute("mostOverdueDays", mostOverdueDays);
        model.addAttribute("overdueUsers", overdueUsers);
        
        return "borrow/overdue";
    }
}