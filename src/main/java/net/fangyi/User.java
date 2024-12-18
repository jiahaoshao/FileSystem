package net.fangyi;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {
    private String username; // 用户名
    private String password; // 密码
    private String group; // 用户组
    private String homeDirectory; // 主目录
}
