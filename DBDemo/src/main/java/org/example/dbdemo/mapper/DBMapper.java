package org.example.dbdemo.mapper;

import org.apache.ibatis.annotations.*;
import org.example.dbdemo.entity.User;

import java.util.List;

@Mapper

public interface DBMapper {
    @Insert("INSERT INTO user_info (username, password) VALUES (#{username}, #{password})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void writeDB(User user);

    @Select("SELECT username, password FROM user_info ORDER BY id")
    List<User> readDB();

    @Select("SELECT username, password FROM user_info ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<User> readByPage(@Param("limit") int limit, @Param("offset") int offset);
}
