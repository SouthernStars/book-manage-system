package com.Southern.book.service;

import com.Southern.book.entity.Book;
import com.Southern.book.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {
    @Autowired
    private BookRepository bookRepository;

    // 获取所有图书
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // 根据ID查找图书
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    // 添加图书
    @Transactional
    public Book addBook(Book book) {
        return bookRepository.save(book);
    }

    // 更新图书
    @Transactional
    public Optional<Book> updateBook(Long id, Book bookDetails) {
        return bookRepository.findById(id).map(book -> {
            book.setTitle(bookDetails.getTitle());
            book.setAuthor(bookDetails.getAuthor());
            book.setIsbn(bookDetails.getIsbn());
            book.setPublisher(bookDetails.getPublisher());
            book.setPublishDate(bookDetails.getPublishDate());
            book.setPrice(bookDetails.getPrice());
            book.setTotalCopies(bookDetails.getTotalCopies());
            book.setAvailableCopies(bookDetails.getAvailableCopies());
            book.setCoverImage(bookDetails.getCoverImage());
            book.setLocation(bookDetails.getLocation());
            book.setDescription(bookDetails.getDescription());
            book.setCategories(bookDetails.getCategories());
            return bookRepository.save(book);
        });
    }

    // 删除图书
    @Transactional
    public boolean deleteBook(Long id) {
        return bookRepository.findById(id).map(book -> {
            bookRepository.delete(book);
            return true;
        }).orElse(false);
    }

    // 搜索图书（简单关键词搜索）
    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllBooks();
        }
        return bookRepository.searchBooks(keyword.trim());
    }
    
    // 多条件搜索图书
    public List<Book> searchBooks(String keyword, Long categoryId, String status) {
        List<Book> books = getAllBooks();
        
        // 应用筛选条件
        if (books != null && !books.isEmpty()) {
            // 关键词筛选
            if (keyword != null && !keyword.trim().isEmpty()) {
                String lowerKeyword = keyword.trim().toLowerCase();
                books = books.stream()
                    .filter(book -> book.getTitle().toLowerCase().contains(lowerKeyword) ||
                            book.getAuthor().toLowerCase().contains(lowerKeyword) ||
                            (book.getIsbn() != null && book.getIsbn().toLowerCase().contains(lowerKeyword)) ||
                            (book.getPublisher() != null && book.getPublisher().toLowerCase().contains(lowerKeyword)))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // 分类筛选
            if (categoryId != null) {
                books = books.stream()
                    .filter(book -> book.getCategories() != null && 
                            book.getCategories().stream()
                                .anyMatch(category -> category.getId().equals(categoryId)))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // 状态筛选
            if (status != null && !status.isEmpty()) {
                boolean isAvailable = "AVAILABLE".equals(status);
                books = books.stream()
                    .filter(book -> (isAvailable && book.getAvailableCopies() > 0) ||
                            (!isAvailable && book.getAvailableCopies() <= 0))
                    .collect(java.util.stream.Collectors.toList());
            }
        }
        
        return books;
    }

    // 获取可借阅的图书
    public List<Book> getAvailableBooks() {
        return bookRepository.findByAvailableCopiesGreaterThan(0);
    }

    // 根据分类获取图书
    public List<Book> getBooksByCategory(Long categoryId) {
        return bookRepository.findByCategoryId(categoryId);
    }

    // 减少可借阅数量
    @Transactional
    public boolean reduceAvailableCopies(Long bookId) {
        return bookRepository.findById(bookId).map(book -> {
            if (book.getAvailableCopies() > 0) {
                book.setAvailableCopies(book.getAvailableCopies() - 1);
                bookRepository.save(book);
                return true;
            }
            return false;
        }).orElse(false);
    }

    // 增加可借阅数量
    @Transactional
    public boolean increaseAvailableCopies(Long bookId) {
        return bookRepository.findById(bookId).map(book -> {
            if (book.getAvailableCopies() < book.getTotalCopies()) {
                book.setAvailableCopies(book.getAvailableCopies() + 1);
                bookRepository.save(book);
                return true;
            }
            return false;
        }).orElse(false);
    }
    
    // 获取图书总数
    public long getTotalBookCount() {
        return bookRepository.count();
    }
    
    // 获取可借阅图书总数
    public long getAvailableBookCount() {
        return bookRepository.findByAvailableCopiesGreaterThan(0).size();
    }
}