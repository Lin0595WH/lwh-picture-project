package com.lwh.pictureproject;

import com.lwh.pictureproject.exception.BusinessException;
import com.lwh.pictureproject.mapper.SpaceMapper;
import com.lwh.pictureproject.model.dto.space.SpaceAddRequest;
import com.lwh.pictureproject.model.entity.Space;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.enums.SpaceLevelEnum;
import com.lwh.pictureproject.service.UserService;
import com.lwh.pictureproject.service.impl.SpaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Lin
 * @version 1.0.0
 * @description 创建空间的单元的测试
 * @date 2025/1/16 21:57
 */
@ExtendWith(MockitoExtension.class)
public class SpaceServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private SpaceMapper spaceMapper;

    @InjectMocks
    private SpaceServiceImpl spaceService;

    private User mockUser;
    private SpaceAddRequest mockSpaceAddRequest;

    @BeforeEach
    public void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockSpaceAddRequest = new SpaceAddRequest();
        mockSpaceAddRequest.setSpaceType(0);
    }

    @Test
    public void addSpace_AdminUser_CreatesAdvancedSpace() {
        mockSpaceAddRequest.setSpaceLevel(SpaceLevelEnum.FLAGSHIP.getValue());
        when(userService.isAdmin(mockUser)).thenReturn(true);

        long spaceId = spaceService.addSpace(mockSpaceAddRequest, mockUser);

        assertNotNull(spaceId);
    }

    @Test
    public void addSpace_NonAdminUser_CreatesCommonSpace() {
        mockSpaceAddRequest.setSpaceLevel(SpaceLevelEnum.FLAGSHIP.getValue());
        when(userService.isAdmin(mockUser)).thenReturn(false);

        assertThrows(BusinessException.class, () -> spaceService.addSpace(mockSpaceAddRequest, mockUser));
    }

    @Test
    public void addSpace_UserAlreadyHasSpace_ThrowsException() {
        mockSpaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        when(userService.isAdmin(mockUser)).thenReturn(true);
        when(spaceMapper.exists(any())).thenReturn(true);

        assertThrows(BusinessException.class, () -> spaceService.addSpace(mockSpaceAddRequest, mockUser));
    }

    @Test
    public void addSpace_SpaceSaveFails_ThrowsException() {
        mockSpaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        when(userService.isAdmin(mockUser)).thenReturn(true);
        when(spaceMapper.exists(any())).thenReturn(false);
        when(spaceMapper.insert(any(Space.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> spaceService.addSpace(mockSpaceAddRequest, mockUser));
    }

    @Test
    public void addSpace_SuccessfulSpaceCreation_ReturnsSpaceId() {
        mockSpaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        when(userService.isAdmin(mockUser)).thenReturn(true);
        when(spaceMapper.exists(any())).thenReturn(false);
        when(spaceMapper.insert(any(Space.class))).thenReturn(1);

        long spaceId = spaceService.addSpace(mockSpaceAddRequest, mockUser);

        assertNotNull(spaceId);
    }
}
