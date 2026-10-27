package org.example.dbdemo.controller;

import org.example.dbdemo.entity.User;
import org.example.dbdemo.service.DBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DBController {
    @Autowired
    private DBService dbService;
    public void setDbService(DBService dbService) {
        this.dbService = dbService;
    }

    @PostMapping("/db")
    public void writeDB(@RequestBody User user) {
        dbService.writeDB(user);
    }
    @GetMapping("/db")
    public List<User> readDB() {
        return dbService.readDB();
    }
    @GetMapping("/db/page")
    public List<User> readByPage(@RequestParam int size, @RequestParam int page) {
        return dbService.readByPage(size, page);
    }
}
