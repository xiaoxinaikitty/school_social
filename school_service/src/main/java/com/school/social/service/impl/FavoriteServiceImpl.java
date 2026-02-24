package com.school.social.service.impl;

import com.school.social.entity.Favorite;
import com.school.social.mapper.FavoriteMapper;
import com.school.social.service.FavoriteService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Resource
    private FavoriteMapper mapper;

    @Override
    public int insert(Favorite entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateById(Favorite entity) {
        return mapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    @Override
    public Favorite selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<Favorite> selectAll() {
        return mapper.selectAll();
    }
}
