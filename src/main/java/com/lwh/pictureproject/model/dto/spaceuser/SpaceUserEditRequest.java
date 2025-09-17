package com.lwh.pictureproject.model.dto.spaceuser;


import lombok.Data;

import java.io.Serializable;

/**
 * @Author Lin
 * @Date 2025/9/15 21:55
 * @Descriptions 编辑空间成员请求
 */
@Data
public class SpaceUserEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
