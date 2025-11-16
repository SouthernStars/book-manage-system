package com.Southern.book.service;

import com.Southern.book.entity.BorrowRecord;
import com.Southern.book.entity.User;
import com.Southern.book.entity.Book;
import com.Southern.book.repository.BorrowRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class BorrowService {
    @Autowired
    private BorrowRecordRepository borrowRecordRepository;
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private UserService userService;

    // 借阅图书
    @Transactional
    public BorrowRecord borrowBook(Long userId, Long bookId, int days) {
        // 检查用户和图书是否存在
        Optional<User> userOpt = userService.getUserById(userId);
        Optional<Book> bookOpt = bookService.getBookById(bookId);
        
        if (!userOpt.isPresent() || !bookOpt.isPresent()) {
            throw new IllegalArgumentException("用户或图书不存在");
        }
        
        User user = userOpt.get();
        Book book = bookOpt.get();
        
        // 检查用户是否已借阅该书
        BorrowRecord activeBorrow = borrowRecordRepository.findActiveBorrowByUserAndBook(userId, bookId);
        if (activeBorrow != null) {
            throw new IllegalStateException("用户已借阅该书");
        }
        
        // 检查可借阅数量
        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("该书已无可用副本");
        }
        
        // 减少图书可借阅数量
        bookService.reduceAvailableCopies(bookId);
        
        // 创建借阅记录
        LocalDate borrowDate = LocalDate.now();
        LocalDate dueDate = borrowDate.plusDays(days);
        BorrowRecord borrowRecord = new BorrowRecord(user, book, borrowDate, dueDate);
        
        return borrowRecordRepository.save(borrowRecord);
    }

    // 归还图书
    @Transactional
    public Optional<BorrowRecord> returnBook(Long recordId) {
        return borrowRecordRepository.findById(recordId).map(record -> {
            if ("RETURNED".equals(record.getStatus())) {
                throw new IllegalStateException("该书已归还");
            }
            
            // 计算罚款
            LocalDate returnDate = LocalDate.now();
            if (returnDate.isAfter(record.getDueDate())) {
                long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), returnDate);
                double finePerDay = 0.5; // 每天罚款0.5元
                record.setFineAmount(overdueDays * finePerDay);
            }
            
            // 更新借阅记录
            record.setReturnDate(returnDate);
            record.setStatus("RETURNED");
            
            // 增加图书可借阅数量
            bookService.increaseAvailableCopies(record.getBook().getId());
            
            return borrowRecordRepository.save(record);
        });
    }

    // 获取所有借阅记录
    public List<BorrowRecord> getAllBorrowRecords() {
        return borrowRecordRepository.findAll();
    }

    // 根据用户获取借阅记录
    public List<BorrowRecord> getBorrowRecordsByUser(Long userId) {
        Optional<User> userOpt = userService.getUserById(userId);
        if (userOpt.isPresent()) {
            return borrowRecordRepository.findByUser(userOpt.get());
        }
        return List.of();
    }

    // 根据用户名获取借阅记录
    public List<BorrowRecord> getBorrowRecordsByUsername(String username) {
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isPresent()) {
            return getBorrowRecordsByUser(userOpt.get().getId());
        }
        return List.of();
    }

    // 获取逾期记录
    public List<BorrowRecord> getOverdueRecords() {
        return borrowRecordRepository.findByDueDateBeforeAndStatusNot(LocalDate.now(), "RETURNED");
    }

    // 获取逾期记录数量
    public int getOverdueCount() {
        return getOverdueRecords().size();
    }

    // 获取活跃借阅记录数量（未归还的借阅记录）
    public int getActiveBorrowCount() {
        List<BorrowRecord> activeRecords = borrowRecordRepository.findByStatusNot("RETURNED");
        return activeRecords.size();
    }

    // 更新逾期状态
    @Transactional
    public void updateOverdueStatus() {
        List<BorrowRecord> overdueRecords = getOverdueRecords();
        for (BorrowRecord record : overdueRecords) {
            if (!"OVERDUE".equals(record.getStatus())) {
                record.setStatus("OVERDUE");
                borrowRecordRepository.save(record);
            }
        }
    }
    
    // 根据ID获取借阅记录
    public Optional<BorrowRecord> getBorrowRecordById(Long id) {
        return borrowRecordRepository.findById(id);
    }
}