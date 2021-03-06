package com.upms.dao.mapper;

import com.upms.dao.model.SysRolePermissionExample;
import com.upms.dao.model.SysRolePermissionKey;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysRolePermissionMapper {
    long countByExample(SysRolePermissionExample example);

    int deleteByExample(SysRolePermissionExample example);

    int deleteByPrimaryKey(SysRolePermissionKey key);

    int insert(SysRolePermissionKey record);

    int insertSelective(SysRolePermissionKey record);

    List<SysRolePermissionKey> selectByExample(SysRolePermissionExample example);

    int updateByExampleSelective(@Param("record") SysRolePermissionKey record, @Param("example") SysRolePermissionExample example);

    int updateByExample(@Param("record") SysRolePermissionKey record, @Param("example") SysRolePermissionExample example);
}