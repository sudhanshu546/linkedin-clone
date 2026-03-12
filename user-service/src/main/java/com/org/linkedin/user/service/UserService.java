package com.org.linkedin.user.service;

import com.org.linkedin.dto.user.ChangePassword;
import com.org.linkedin.dto.user.TUserDTO;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

  List<TUserDTO> searchUsers(String query);

  void save(@Valid TUserDTO employeeDTO, String clientName);

  TUserDTO getUserDetails(UUID fromString);

  void delete(String clientName, UUID id);

  Page<TUserDTO> getAllUserDetail(Pageable pageable);

  Optional<TUserDTO> findOne(UUID id);

  void resetPassword(ChangePassword changePassword, String clientName);

  void updateUserById(UUID empId, MultipartFile image, TUserDTO employeeDTO) throws IOException;

  String sendForgotLink(String email) throws Exception;

  void forgotPassword(String token, ChangePassword changePassword) throws Exception;

  void saveImage(MultipartFile file, Authentication authentication) throws IOException;

  byte[] getImage(Authentication authentication);

  TUserDTO findUserByKeyCloakId(UUID keyCloakId);

  TUserDTO getUserDetailsByAuthentication(Authentication authentication);
}
