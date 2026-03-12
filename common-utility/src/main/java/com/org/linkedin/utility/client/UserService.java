package com.org.linkedin.utility.client;

import com.org.linkedin.dto.BaseResponse;
import com.org.linkedin.dto.user.TUserDTO;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/us")
public interface UserService {

  @GetMapping("/user/user/{id}")
  public ResponseEntity<BaseResponse<TUserDTO>> getUserByKeyCloakId(
      @PathVariable("id") UUID keyCloakId);

  @GetMapping("/user/{id}")
  public ResponseEntity<BaseResponse<TUserDTO>> getUserByInternalId(
      @PathVariable("id") UUID internalId);

  @GetMapping("/posts/{postId}/is-liked")
  public ResponseEntity<Boolean> isLiked(@PathVariable("postId") UUID postId);
}
