package com.Southern.book.repository;

import com.Southern.book.entity.BorrowRecord;
import com.Southern.book.entity.User;
import com.Southern.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    List<BorrowRecord> findByUser(User user);
    List<BorrowRecord> findByBook(Book book);
    List<BorrowRecord> findByStatus(String status);
    List<BorrowRecord> findByDueDateBeforeAndStatusNot(LocalDate date, String status);
    
    @Query("SELECT br FROM BorrowRecord br WHERE br.user.id = :userId AND br.book.id = :bookId AND br.status = 'BORROWED'")
    BorrowRecord findActiveBorrowByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    
    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.user.id = :userId AND br.status = 'BORROWED'")
    int countActiveBorrowsByUser(@Param("userId") Long userId);
    
    List<BorrowRecord> findByStatusNot(String status);
}