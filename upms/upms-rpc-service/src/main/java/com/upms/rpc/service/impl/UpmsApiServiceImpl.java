package com.upms.rpc.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.upms.rpc.service.model.User;
import com.upms.rpc.service.util.Securitys;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UnknownAccountException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.config.annotation.Service;
import com.google.common.collect.Maps;
import com.upms.common.util.PasswordUtils;
import com.upms.dao.mapper.*;
import com.upms.dao.model.*;
import com.upms.rpc.api.UpmsApiService;

@Service(version = "1.0.0",
        application = "${dubbo.application.id}",
        protocol = "${dubbo.protocol.id}",
        registry = "${dubbo.registry.id}")
public class UpmsApiServiceImpl implements UpmsApiService {
	@Autowired
	private SysAccountMapper		sysAccountMapper;
	@Autowired
	private SysAccountRoleMapper	sysAccountRoleMapper;
	@Autowired
	private SysMenuMapper			sysMenuMapper;
	@Autowired
	private SysPermissionMapper		sysPermissionMapper;
	@Autowired
	private SysRoleMapper			sysRoleMapper;
	@Autowired
	private SysRolePermissionMapper	sysRolePermissionMapper;



	@Override
	public ShiroUser login(String username, String password) {
		SysAccountExample exp = new SysAccountExample();
		exp.createCriteria().andLoginNameEqualTo(username);
		List<SysAccount> accounts = sysAccountMapper.selectByExample(exp);
		if (accounts.size() > 0) {
			SysAccount account = accounts.get(0);
			String currPassword = PasswordUtils.getEncodePassWord(password, PasswordUtils.decodeHex(account.getSalt()));
			if (!account.getPassword().equals(currPassword)) {
				throw new AuthenticationException("password is  not correct");
			}
		} else {
			throw new UnknownAccountException("username is not correct");
		}
		SysAccount account = accounts.get(0);
		ShiroUser shiroUser = new ShiroUser();
		shiroUser.setAccountId(account.getId());
		shiroUser.setAdmin(account.getIsAdmin() == 1);
		shiroUser.setLoginName(account.getLoginName());
		shiroUser.setName(account.getName());
		setShiroUserExtraInfo(shiroUser);
		return shiroUser;
	}



	@Override
	public void setShiroUserExtraInfo(ShiroUser shiroUser) {
		List<SysRole> roles;
		List<SysPermission> permissions;
		List<SysMenu> menus;
		if (shiroUser.isAdmin()) {
			roles = sysRoleMapper.selectByExample(null);
			permissions = sysPermissionMapper.selectByExample(null);
			menus = sysMenuMapper.selectByExample(null);
		} else {
			// query roles
			SysAccountRoleExample sysAccountRoleExample = new SysAccountRoleExample();
			sysAccountRoleExample.createCriteria().andAccountIdEqualTo(shiroUser.getAccountId());
			List<SysAccountRoleKey> sysAccountRoleKeys = sysAccountRoleMapper.selectByExample(sysAccountRoleExample);

			SysRoleExample roleExample = new SysRoleExample();
			List<String> roleIds = sysAccountRoleKeys.stream().map(SysAccountRoleKey::getRoleId)
					.collect(Collectors.toList());
			roleExample.createCriteria().andIdIn(roleIds);
			if (roleIds.size() == 0) {
				throw new AuthenticationException("role permission deny");
			}

			roles = sysRoleMapper.selectByExample(roleExample);
			// query permissions
			SysRolePermissionExample rolePermissionExample = new SysRolePermissionExample();
			rolePermissionExample.createCriteria().andRoleIdIn(roleIds);
			List<SysRolePermissionKey> sysRolePermissionKeys = sysRolePermissionMapper
					.selectByExample(rolePermissionExample);
			List<String> permissionsIds = sysRolePermissionKeys.stream().map(SysRolePermissionKey::getPermissionId)
					.collect(Collectors.toList());
			SysPermissionExample sysPermissionExample = new SysPermissionExample();
			sysPermissionExample.createCriteria().andIdIn(permissionsIds);
			permissions = sysPermissionMapper.selectByExample(sysPermissionExample);

			// query menus
			SysMenuExample menuExample = new SysMenuExample();
			menuExample.setOrderByClause("MENU_LEVEL, SEQUENCE asc");
			menuExample.createCriteria().andPermissionIn(permissionsIds);
			menus = sysMenuMapper.selectByExample(menuExample);
		}
		List<String> roleStr = roles.stream().map(SysRole::getName).collect(Collectors.toList());
		List<String> permissionsStr = permissions.stream().map(SysPermission::getCode).collect(Collectors.toList());

		shiroUser.setRoles(roleStr);
		shiroUser.setPermissions(permissionsStr);
		shiroUser.setMenus(menus.stream().map(menu -> {
			Menu newMenu = new Menu();
			BeanUtils.copyProperties(menu, newMenu);
			return newMenu;
		}).collect(Collectors.toList()));
	}



	@Override
	public Map<String, Object> getUser() {
		Map<String, Object> res = Maps.newHashMap();
		res.put("user", User.buildUser());
		if (Securitys.isAuthenticatedOrRemembered() && Securitys.getMenus() == null) {
			setShiroUserExtraInfo(Securitys.getUser());
		}
		res.put("menu", Securitys.getMenus());
		return res;
	}
}
