package org.example.dbdemo.service;

import org.example.dbdemo.entity.User;
import org.example.dbdemo.mapper.DBMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DBService {

    @Autowired
    private DBMapper dbMapper;
    public void setDbMapper(DBMapper dbMapper) {
        this.dbMapper = dbMapper;
    }
    public void writeDB(User user) {
        dbMapper.writeDB(user);
    }
    public List<User> readDB() {
        return dbMapper.readDB();
    }
    public List<User> readByPage(int limit, int page) {
        int offset = (page - 1) * limit;
        return dbMapper.readByPage(limit, offset);
    }
}
