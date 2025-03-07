package com.sansantek.sansanmulmul.crew.controller;

import com.sansantek.sansanmulmul.crew.domain.Crew;
import com.sansantek.sansanmulmul.crew.dto.request.CrewCreateRequest;
import com.sansantek.sansanmulmul.crew.dto.response.CrewAlarmResponse;
import com.sansantek.sansanmulmul.crew.dto.response.CrewGalleryResponse;
import com.sansantek.sansanmulmul.crew.dto.response.crewdetail.CrewDetailCommonResponse;
import com.sansantek.sansanmulmul.crew.dto.response.crewdetail.CrewDetailResponse;
import com.sansantek.sansanmulmul.crew.dto.response.CrewMyResponse;
import com.sansantek.sansanmulmul.crew.dto.response.CrewResponse;
import com.sansantek.sansanmulmul.crew.dto.response.crewdetail.CrewHikingDetailResponse;
import com.sansantek.sansanmulmul.crew.dto.response.crewdetail.CrewUserResponse;
import com.sansantek.sansanmulmul.crew.service.CrewService;
import com.sansantek.sansanmulmul.crew.service.request.CrewRequestService;
import com.sansantek.sansanmulmul.exception.auth.InvalidTokenException;
import com.sansantek.sansanmulmul.exception.style.GroupNotFoundException;
import com.sansantek.sansanmulmul.user.domain.User;
import com.sansantek.sansanmulmul.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/crew")
@Tag(name = "그룹 컨트롤러", description = "그룹 정보관련 기능 수행")
public class CrewController {

    /*
    * 1. 그룹 전체 조회
    * 2. 그룹 생성
    * 3. 그룹 상세 보기
    * 4. 그룹 상세 보기 - 그룹 갤러리
    * 5. 그룹 알림 보기
    * 
    * 1. 그룹 내 멤버들
    * 2. '내' 진행 중 그룹
    * 3. '내' 완료된 그룹
    * */

    // service
    private final CrewService crewService;
    private final UserService userService;

    private final CrewRequestService crewRequestService;


    /* 1. 그룹 전체 조회 */
    @GetMapping("/all")
    @Operation(summary = "그룹 전체 목록 조회", description = "그룹 전체 목록 정보 조회")
    public ResponseEntity<?> getAllCrews(Authentication authentication) {
        HttpStatus status = HttpStatus.OK;

        try {
            // 전체 그룹 가져오기
            String userProviderId = authentication.getName();
            List<CrewResponse> crewResponse = crewService.getAllCrews(userProviderId);

            return new ResponseEntity<>(crewResponse, status);

        } catch (GroupNotFoundException e) {
            status = HttpStatus.NOT_FOUND; // 404

            return new ResponseEntity<>(e.getMessage(), status);
        } catch (Exception e) {
            status = HttpStatus.BAD_REQUEST; // 400

            return new ResponseEntity<>(e.getMessage(), status);
        }
    }

/*    @GetMapping("/{styleId}")
    @Operation(summary = "그룹 특정 등산 스타일 전체 조회", description = "해당 등산 스타일에 해당하는 그룹 전체 조회")
    public ResponseEntity<?> getCrewsbyStyle(@PathVariable("styleId") int styleId, Authentication authentication) {
//        Map<String, Object> resultMap = new HashMap<>();
        HttpStatus status = HttpStatus.ACCEPTED;

        try {
            //유저
            String userProviderId = authentication.getName();

            // 특정 스타일에 해당하는 그룹 정보 조회
            List<CrewResponse> crewStyleResponseList = crewService.getCrewListbyStyle(styleId, userProviderId);

            // JSON으로 결과 전송
//            resultMap.put("crewStyleResponseList", crewStyleResponseList);

            return new ResponseEntity<>(crewStyleResponseList, status);

        } catch ( Exception e ) {
            status = HttpStatus.BAD_REQUEST; // 400

            return new ResponseEntity<>(e.getMessage(), status);
        }
    }*/

    ////////////////////////////////////////////////////////////

    /* 2. 그룹 생성 */
    @PostMapping
    @Operation(summary = "그룹 생성", description = "해당 사용자 그룹 생성")
    public ResponseEntity<?> createCrew
    (Authentication authentication,
     @RequestBody CrewCreateRequest crewCreateRequest) {
        HttpStatus status = HttpStatus.ACCEPTED;

        try {

            // 1. 그룹을 생성하는 사용자 가져오기
            // 토큰을 통해 userProvider 추출
            String userProviderId = authentication.getName();
            // 해당 사용자 가져오기
            User user = userService.getUser(userProviderId);

            // 2. 해당 사용자 userId로 그룹 생성 - 방장(해당 사용자는 방장이 됨)
            Crew crew = crewService.createCrew(user.getUserId(), crewCreateRequest);

            // 3. 그룹 등산 스타일 추가
            crewService.addStyles(crew, crewCreateRequest.getCrewStyles());

            // 4. crewuser 테이블에 방장도 추가
            crewService.updateCrewUser(crew, user.getUserId());

            // 5. 그룹 채팅방 (미완)

            status = HttpStatus.CREATED; // 201

            return new ResponseEntity<>(status);

        } catch (InvalidTokenException e) {

            log.error("토큰 유효성 검사 실패: {}", e.getMessage());
            status = HttpStatus.UNAUTHORIZED; // 401

            return new ResponseEntity<>(e.getMessage(), status);
        } catch (Exception e) {

            log.error("그룹 생성 실패: {}", e.getMessage());
            status = HttpStatus.BAD_REQUEST; // 400

            return new ResponseEntity<>(e.getMessage(), status);
        }
    }



    /* 3. 그룹 상세 보기 */
    // (1) 상단 공통된 부분 (유저 정보 isLeader 포함)
    @GetMapping("/detail/{crewId}/common")
    @Operation(summary = "그룹 상세 조회 [상단공통부분]", description = "그룹 상세보기 - 상단에 고정되는 부분")
    public ResponseEntity<?> getCrewDetailCommon(Authentication authentication, @PathVariable int crewId) {
        HttpStatus status = HttpStatus.OK;

        try {
            // 1. 현재 사용자 가져오기
            String userProviderId = authentication.getName(); // 토큰을 통해 userProvider 추출
            User user = userService.getUser(userProviderId); // 해당 사용자 가져오기

            // 2. 그룹 정보 가져오기
            CrewDetailCommonResponse crewDetailCommonResponse = crewService.getCrewDetailCommon(crewId, user.getUserId());

            return new ResponseEntity<>(crewDetailCommonResponse, status);
        } catch (Exception e) {

            status = HttpStatus.BAD_REQUEST; // 400

            return new ResponseEntity<>(e.getMessage(), status);
        }
    }


    // (2) [탭1] 그룹 정보
    @GetMapping("/detail/{crewId}/info")
    @Operation(summary = "그룹 상세 조회 [탭1] 그룹 정보", description = "그룹 상세보기 - (탭1) 그룹 정보")
    public ResponseEntity<?> getCrewDetailCrewInfo( @PathVariable int crewId) {
        HttpStatus status = HttpStatus.OK;

        try {

            CrewDetailResponse crewDetailResponse = crewService.getCrewDetailCrewInfo(crewId);

            return new ResponseEntity<>(crewDetailResponse, status);
        } catch (Exception e) {

            status = HttpStatus.BAD_REQUEST; // 400

            return new ResponseEntity<>(e.getMessage(), status);
        }
    }

    // (3) [탭2] 등산 정보
    @GetMapping("/detail/{crewId}/hikinginfo")
    @Operation(summary = "그룹 상세 조회 [탭2] 등산 정보", description = "그룹 상세보기 - (탭2) 등산 정보")
    public ResponseEntity<?> getCrewDetailHikingInfo(@PathVariable int crewId) {
        HttpStatus status = HttpStatus.OK;

        try {
            CrewHikingDetailResponse crewHikingDetailResponse = crewService.getCrewDetailHikingInfo(crewId);

            return new ResponseEntity<>(crewHikingDetailResponse, status);
        } catch (Exception e) {

            status = HttpStatus.BAD_REQUEST; // 400

            return new ResponseEntity<>(e.getMessage(), status);
        }
    }

    /* 4. [탭3] 그룹 갤러리 */
    // 4-1. 이미지 업로드
    @PostMapping(value = "/detail/{crewId}/gallery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "그룹 상세 조회 [탭3] 갤러리 사진 '업로드'",  description = "그룹 상세보기 - 그룹 갤러리에 사진 업로드")
    public ResponseEntity<?> uploadImg(Authentication authentication,
                                       @PathVariable int crewId,
                                       @RequestPart(value = "image") MultipartFile image) throws IOException {
        HttpStatus status = HttpStatus.OK;
        try {
            //사용자 정보
            String userProviderId = authentication.getName();
            User user = userService.getUser(userProviderId);

            boolean flag = crewService.uploadImg(crewId, user, image);

            return new ResponseEntity<>(flag, status);
        } catch (Exception e) {
            status = HttpStatus.BAD_REQUEST; // 400

            return new ResponseEntity<>(e.getMessage(), status);
        }

    }

    // 4-2. 갤러리 전부 가져오기
    @GetMapping(value = "/detail/{crewId}/gallery")
    @Operation(summary = "그룹 상세 조회 [탭3] 그룹 갤러리 '전체 조회'",  description = "그룹 상세보기 - (탭3) 그룹 갤러리")
    public ResponseEntity<?> getCrewDetailGallery(Authentication authentication, @PathVariable int crewId) {
        HttpStatus status = HttpStatus.OK;
        try {
            //사용자 정보
            String userProviderId = authentication.getName();
            User user = userService.getUser(userProviderId);

            List<CrewGalleryResponse> crewGalleryResponse = crewService.getCrewDetailGallery(crewId, user);
            return new ResponseEntity<>(crewGalleryResponse, status); //ok 200

        } catch (Exception e) {
            status = HttpStatus.BAD_REQUEST; // 400

            return new ResponseEntity<>(e.getMessage(), status);
        }
    }

    // 4-3. 이미지 삭제
    @DeleteMapping(value="/detail/{crewId}/gallery")
    @Operation(summary = "그룹 상세 조회 [탭3] 그룹 갤러리 '이미지 삭제'", description = "본인이 올린 사진 삭제")
    public ResponseEntity<?> deleteCrewDetailGallery(Authentication authentication,
                                                     @PathVariable int crewId,
                                                    @RequestParam int picId) {
        HttpStatus status = HttpStatus.OK;
        try {
            String userProviderId = authentication.getName();
            User user = userService.getUser(userProviderId);

            boolean flag = crewService.deleteImg(crewId, user, picId);

            return new ResponseEntity<>(flag, status);

        } catch (Exception e) {
            status = HttpStatus.BAD_REQUEST; // 400

            return new ResponseEntity<>(e.getMessage(), status);
        }

    }

    /* 5. 그룹 알림 보기 */
    @GetMapping(value="/detail/{crewId}/alarm")
    @Operation(summary = "그룹 내 알림 조회", description = "가입요청/방장위임 알림")
    public ResponseEntity<?> getCrewDetailAlarm(@PathVariable int crewId) {
        HttpStatus status = HttpStatus.OK;
        try {
            List<CrewAlarmResponse> crewAlarmResposne = crewService.getCrewDetailAlarm(crewId);
            return new ResponseEntity<>(crewAlarmResposne, HttpStatus.OK);
        } catch (Exception e) {
            status = HttpStatus.BAD_REQUEST; // 400

            return new ResponseEntity<>(e.getMessage(), status);
        }
    }


    ////////////////////////////////////////////////////////////

    /* 1. 그룹 내 멤버들 */  //-> 위에 [탭1] 그룹정보로 통합
//    @GetMapping("/member/{crewId}")
//    public ResponseEntity<?> getCrewMembers(@PathVariable int crewId) {
//        try {
//            List<CrewUserResponse> members = crewRequestService.getCrewMembers(crewId);
//            return ResponseEntity.ok(members);
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }

    /* [그룹 목록] // 내거 보여주는 목록 */
    /* 2. '내' 진행 중 그룹 */
    @Operation(summary = "나의 진행 중인 그룹 조회", description = "회원의 진행 중인 그룹을 조회합니다.")
    @GetMapping("/ing")
    public ResponseEntity<?> getingCrews(Authentication authentication) {
        try {
            String userProviderId = authentication.getName();
            User user = userService.getUser(userProviderId);

            List<CrewResponse> crewResponse = crewService.getMyOnGoingCrews(user);

            return ResponseEntity.ok(crewResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /* 3. '내' 완료된 그룹 */
    @Operation(summary = "나의 종료된 그룹 조회", description = "회원의 종료된 그룹을 조회합니다.")
    @GetMapping("/complete")
    public ResponseEntity<?> getCompletedCrews(Authentication authentication) {
        try {
            String userProviderId = authentication.getName();
            User user = userService.getUser(userProviderId);

            List<CrewResponse> crewResponse = crewService.getMyCompletedCrews(user);

            return ResponseEntity.ok(crewResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}