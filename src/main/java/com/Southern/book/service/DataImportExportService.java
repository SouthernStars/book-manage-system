package com.Southern.book.service;

import com.Southern.book.entity.Book;
import com.Southern.book.entity.User;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataImportExportService {
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private UserService userService;
    
    // 公共方法，用于获取所有图书
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }
    
    // 公共方法，用于获取所有用户
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 导出图书数据为CSV
    public File exportBooksToCsv() {
        try {
            File tempFile = File.createTempFile("books_", ".csv");
            try (CSVWriter writer = new CSVWriter(new FileWriter(tempFile))) {
                // 写入表头
                String[] header = {"ID", "ISBN", "Title", "Author", "Publisher", "PublishDate", "Price", "TotalCopies", "AvailableCopies"};
                writer.writeNext(header);

                // 写入数据
                List<Book> books = bookService.getAllBooks();
                for (Book book : books) {
                    String[] data = {
                        book.getId().toString(),
                        book.getIsbn() != null ? book.getIsbn() : "",
                        book.getTitle(),
                        book.getAuthor() != null ? book.getAuthor() : "",
                        book.getPublisher() != null ? book.getPublisher() : "",
                        book.getPublishDate() != null ? book.getPublishDate().format(DATE_FORMATTER) : "",
                        book.getPrice() != null ? book.getPrice().toString() : "0",
                        book.getTotalCopies().toString(),
                        book.getAvailableCopies().toString()
                    };
                    writer.writeNext(data);
                }
            }
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("导出CSV失败", e);
        }
    }

    // 从CSV导入图书数据
    public int importBooksFromCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        int importedCount = 0;
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] line;
            boolean isFirstLine = true;

            while ((line = reader.readNext()) != null) {
                // 跳过表头
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // 创建图书对象，字段映射与模板格式匹配："书名", "作者", "出版社", "出版日期", "ISBN", "分类", "总册数", "可借册数", "价格", "描述"
                Book book = new Book();
                if (line.length > 0 && !line[0].isEmpty()) book.setTitle(line[0]); // 书名
                if (line.length > 1 && !line[1].isEmpty()) book.setAuthor(line[1]); // 作者
                if (line.length > 2 && !line[2].isEmpty()) book.setPublisher(line[2]); // 出版社
                if (line.length > 3 && !line[3].isEmpty()) {
                    try {
                        book.setPublishDate(LocalDate.parse(line[3], DATE_FORMATTER)); // 出版日期
                    } catch (Exception e) {
                        // 日期格式错误，跳过
                        System.err.println("日期格式错误: " + line[3]);
                    }
                }
                // 确保ISBN作为字符串处理，避免科学计数法问题
                if (line.length > 4 && !line[4].isEmpty()) {
                    // 去除可能的前后空白并确保为字符串格式
                    String isbn = line[4].trim();
                    // 处理Excel可能自动转换的数字格式
                    if (isbn.contains("E+")) {
                        // 如果是科学计数法格式，尝试转换为普通数字字符串
                        try {
                            BigDecimal bd = new BigDecimal(isbn);
                            isbn = bd.toPlainString();
                        } catch (Exception e) {
                            // 如果转换失败，使用原始值
                        }
                    } else if (isbn.matches("^\\d*\\.?\\d+$") && isbn.contains(".")) {
                        // 如果是小数格式，去除末尾的0和小数点
                        isbn = isbn.replaceAll("\\.?0+$", "");
                    }
                    book.setIsbn(isbn);
                }
                // 分类字段暂时不处理，需要额外的分类服务
                if (line.length > 6 && !line[6].isEmpty()) {
                    try {
                        book.setTotalCopies(Integer.parseInt(line[6])); // 总册数
                    } catch (Exception e) {
                        book.setTotalCopies(1);
                    }
                }
                if (line.length > 7 && !line[7].isEmpty()) {
                    try {
                        book.setAvailableCopies(Integer.parseInt(line[7])); // 可借册数
                    } catch (Exception e) {
                        book.setAvailableCopies(1);
                    }
                }
                if (line.length > 8 && !line[8].isEmpty()) {
                    try {
                        book.setPrice(Double.parseDouble(line[8])); // 价格
                    } catch (Exception e) {
                        // 价格格式错误，设为0
                        book.setPrice(0.0);
                    }
                }
                if (line.length > 9 && !line[9].isEmpty()) book.setDescription(line[9]); // 描述

                // 保存图书
                try {
                    bookService.addBook(book);
                    importedCount++;
                } catch (Exception e) {
                    System.err.println("保存图书失败: " + book.getTitle() + ", 错误: " + e.getMessage());
                    // 继续处理下一行，不中断整个导入过程
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取CSV文件失败", e);
        } catch (com.opencsv.exceptions.CsvValidationException e) {
            throw new RuntimeException("CSV格式验证失败", e);
        }

        return importedCount;
    }
    public File exportUsersToCsv() {
        try {
            File tempFile = File.createTempFile("users_", ".csv");
            try (CSVWriter writer = new CSVWriter(new FileWriter(tempFile))) {
                // 写入用户表头
                String[] header = {"ID", "Username", "Email", "FullName", "Roles", "Enabled"};
                writer.writeNext(header);

                // 获取用户数据并写入
                // List<User> users = userService.getAllUsers();
                // for (User user : users) {
                //     String[] data = {
                //         user.getId().toString(),
                //         user.getUsername(),
                //         user.getEmail(),
                //         user.getFullName(),
                //         user.getRoles().stream().map(Role::getName).collect(Collectors.joining(",")),
                //         String.valueOf(user.isEnabled())
                //     };
                //     writer.writeNext(data);
                // }
            }
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("导出用户CSV失败", e);
        }
    }

    public int importUsersFromCsv(MultipartFile file) {
        // 实现用户导入逻辑
        // 需要先创建 UserService 和相关的实体类
        return 0;
    }

}