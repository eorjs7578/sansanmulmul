package com.sansantek.sansanmulmul.record.service;

import com.sansantek.sansanmulmul.common.util.FcmMessage;
import com.sansantek.sansanmulmul.common.util.FcmType;
import com.sansantek.sansanmulmul.common.util.FcmUtil;
import com.sansantek.sansanmulmul.crew.domain.Crew;
import com.sansantek.sansanmulmul.crew.domain.crewuser.CrewUser;
import com.sansantek.sansanmulmul.crew.dto.response.crewdetail.CrewUserResponse;
import com.sansantek.sansanmulmul.crew.repository.CrewRepository;
import com.sansantek.sansanmulmul.crew.repository.request.CrewUserRepository;
import com.sansantek.sansanmulmul.crew.service.request.CrewRequestService;
import com.sansantek.sansanmulmul.exception.auth.UserNotFoundException;
import com.sansantek.sansanmulmul.exception.record.RecordNotFoundException;
import com.sansantek.sansanmulmul.mountain.domain.Mountain;
import com.sansantek.sansanmulmul.mountain.domain.course.Course;
import com.sansantek.sansanmulmul.mountain.repository.MountainRepository;
import com.sansantek.sansanmulmul.mountain.repository.course.CourseRepository;
import com.sansantek.sansanmulmul.mountain.service.course.CourseService;
import com.sansantek.sansanmulmul.record.domain.HikingRecord;
import com.sansantek.sansanmulmul.record.dto.request.LocationRequest;
import com.sansantek.sansanmulmul.record.dto.request.RecordRequest;
import com.sansantek.sansanmulmul.record.dto.response.AllRecordResonse;
import com.sansantek.sansanmulmul.record.dto.response.DetailRecordResponse;
import com.sansantek.sansanmulmul.record.dto.response.LocationResponse;
import com.sansantek.sansanmulmul.record.repository.HikingRecordRepository;
import com.sansantek.sansanmulmul.user.domain.User;
import com.sansantek.sansanmulmul.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordService {

    // repository
    private final UserRepository userRepository;
    private final HikingRecordRepository hikingRecordRepository;
    private final MountainRepository mountainRepository;
    private final CourseRepository courseRepository;
    private final CrewUserRepository crewUserRepository;
    private final CrewRepository crewRepository;

    // service
    private final CrewRequestService crewRequestService;
    private final CourseService courseService;
    private final FcmUtil fcmUtil;

    // 회원 좌표 저장
    public void saveCoord(int userId, LocationRequest request) {
        // 해당 회원 확인
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException());

        // 해당 그룹 확인
        Crew crew = crewRepository.findByCrewId(request.getCrewId())
                .orElseThrow(() -> new RuntimeException("해당 그룹을 찾을 수 없습니다."));

        // 해당 사용자와 그룹에 해당하는 CrewUser 엔티티 검색
        CrewUser crewUser = crewUserRepository.findByUserAndCrew(user, crew)
                .orElseThrow(() -> new RuntimeException("해당 사용자가 이 그룹에 존재하지 않습니다."));

        // 좌표 저장
        crewUser.setUserLat(request.getUserLat());
        crewUser.setUserLon(request.getUserLon());

        // 변경된 엔티티 저장
        crewUserRepository.save(crewUser);
    }

    // 회원이 참여한 그룹의 멤버들의 좌표 넘기기
    public List<LocationResponse> sendMembersCoord(int crewId) {
        List<LocationResponse> response = new ArrayList<>();

        // 멤버 조회
        List<CrewUser> crewMembers = crewRequestService.getCrewMembersInfo(crewId);

        // 멤버들의 좌표 가져오기
        for (CrewUser cu : crewMembers) {
            LocationResponse lr = new LocationResponse(
                    cu.getUser().getUserId(),
                    cu.getUser().getUserNickname(),
                    cu.getUser().getUserProfileImg(),
                    cu.getUserLat(),
                    cu.getUserLon()
            );

            response.add(lr);
        }

        return response;
    }

    // 회원의 개인 기록 만들기
    @Transactional
    public int addRecords(int userId, RecordRequest request) {
        // 해당 회원 확인
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException());

        // 해당 그룹 확인
        Crew crew = crewRepository.findByCrewId(request.getCrewId())
                .orElseThrow(() -> new RuntimeException("해당 그룹을 찾을 수 없습니다."));

        // 그룹 종료 처리 -> 방장인 경우에만
        if(user.getUserId() == crew.getLeader().getUserId()) {
            crew.setCrewIsDone(true);

            crewRepository.save(crew);
        }

        // 산 확인
        Mountain mountain = mountainRepository.findByMountainId(crew.getMountain().getMountainId())
                .orElseThrow(() -> new RuntimeException("해당 산을 찾을 수 없습니다."));
        
        // 총 걸린 시간 계산
        LocalDateTime recordStartTime = request.getRecordStartTime();
        LocalDateTime recordEndTime = request.getRecordEndTime();

        // Duration을 사용하여 두 시간 사이의 차이를 계산
        Duration duration = Duration.between(recordStartTime, recordEndTime);

        // getMinutes() 메서드를 사용하여 차이를 분 단위로 변환
        long totalMinutes = duration.toMinutes();

        // 기록 객체 생성
        HikingRecord record = new HikingRecord(
                crew,                // crew 설정
                mountain,            // mountain 설정
                user,                // user 설정
                recordStartTime,
                recordEndTime,
                request.getRecordDistance(),
                totalMinutes,
                request.getRecordSteps(),
                request.getRecordElevation(),
                request.getRecordKcal()
        );

        // 기록 영속성
        hikingRecordRepository.save(record);

        // 회원의 records 리스트에 추가
        user.getRecords().add(record);

        // 회원 영속성
        userRepository.save(user);

        // 생성된 기록의 recordId를 반환
        return record.getRecordId();
    }

    // 회원 전체의 기록 가져오기
    public List<AllRecordResonse> getAllRecords(int userId) {
        List<AllRecordResonse> records = new ArrayList<>();

        // 해당 회원의 userId로 등산 기록 찾기
        List<HikingRecord> userRecords = hikingRecordRepository.findByUser_UserId(userId);

        // RecordResonse로 변환
        for (HikingRecord record : userRecords) {
            AllRecordResonse response = new AllRecordResonse(
                    record.getRecordId(),
                    record.getCrew().getCrewId(),
                    record.getMountain().getMountainName(),
                    record.getRecordStartTime(),
                    record.getRecordEndTime(),
                    record.getMountain().getMountainImg()
            );

            records.add(response);
        }

        return records;
    }

    // 회원의 특정 기록 가져오기
    public DetailRecordResponse getDetailRecord(int recordId) {
        // 기록 조회
        HikingRecord hikingRecord = hikingRecordRepository.findByRecordId(recordId)
                .orElseThrow(() -> new RecordNotFoundException());

        // 코스 정보 조회
        Course upCourse = courseRepository.findByCourseId(hikingRecord.getCrew().getUpCourse().getCourseId());
        Course downCourse = courseRepository.findByCourseId(hikingRecord.getCrew().getDownCourse().getCourseId());

        // 멤버 조회
        List<CrewUserResponse> crewMembers = crewRequestService.getCrewMembers(hikingRecord.getCrew().getCrewId());

        // TrackPaths 정보 조회
        Map<String, Object> upCourseDetail = courseService.getCourseDetail(upCourse.getMountain().getMountainId(), upCourse.getCourseId());
        Map<String, Object> downCourseDetail = courseService.getCourseDetail(downCourse.getMountain().getMountainId(), downCourse.getCourseId());

        // TrackPaths만 가져오기
        List<Map<String, Object>> upTrackPaths = (List<Map<String, Object>>) upCourseDetail.get("tracks");
        List<Map<String, Object>> downTrackPaths = (List<Map<String, Object>>) downCourseDetail.get("tracks");

        // 응답 객체 생성
        return new DetailRecordResponse(
                // 산
                hikingRecord.getMountain().getMountainName(),
                hikingRecord.getMountain().getMountainImg(),

                // 코스
                upCourse.getCourseName(),
                downCourse.getCourseName(),
                upCourse.getCourseLength(),
                downCourse.getCourseLength(),

                // TrackPaths 정보 추가
                upTrackPaths,
                downTrackPaths,

                hikingRecord.getCrew().getCrewId(),
                // 참여 멤버
                crewMembers,

                // 기록
                hikingRecord.getRecordStartTime(),
                hikingRecord.getRecordEndTime(),
                hikingRecord.getRecordDistance(),
                hikingRecord.getRecordDuration(),
                hikingRecord.getRecordSteps(),
                hikingRecord.getRecordElevation(),
                hikingRecord.getRecordKcal()
        );
    }

    // 해당 그룹의 방장 유무 확인
    public boolean chkLeader(String userProviderId, int crewId) {
        // 해당 회원 확인
        User user = userRepository.findByUserProviderId(userProviderId)
                .orElseThrow(() -> new UserNotFoundException());

        // 해당 그룹 확인
        Crew crew = crewRepository.findByCrewId(crewId)
                .orElseThrow(() -> new RuntimeException("해당 그룹을 찾을 수 없습니다."));

        if (crew.getLeader().getUserId() == user.getUserId())
            return true;
        else
            return false;
    }

    // 해당 그룹의 관한 기록 여부 확인
    public boolean chkRecord(String userProviderId, int crewId) {
        // 해당 회원 확인
        User user = userRepository.findByUserProviderId(userProviderId)
                .orElseThrow(() -> new UserNotFoundException());

        // 해당 그룹 확인
        Crew crew = crewRepository.findByCrewId(crewId)
                .orElseThrow(() -> new RuntimeException("해당 그룹을 찾을 수 없습니다."));

        // 해당 사용자와 그룹에 대한 HikingRecord 존재 여부 확인
        return hikingRecordRepository.existsByUserAndCrew(user, crew);
    }

    //이탈 알림 crew 모두에게 보내기
    // FcmDTO 생성
    public boolean sendAlarm(int crewId) {
        Crew crew = crewRepository.findByCrewId(crewId)
                .orElseThrow(() -> new RuntimeException("해당 그룹을 찾을 수 없습니다."));
        // FcmDTO 생성
        String title = "\uD83D\uDEA8이탈 감지\uD83D\uDEA8";
        String body = "팀원의 이탈이 감지되었습니다. 위치를 확인하세요!";

        FcmMessage.FcmDTO fcmDTO = fcmUtil.makeFcmDTO(title, body);
        // FCM발송
        fcmSendtoCrew(crew, fcmDTO);
        System.out.println(title);

        return true;
    }

    // 그룹 내 회원들에게 전체 알림
    public void fcmSendtoCrew(Crew crew, FcmMessage.FcmDTO fcmDTO){

        List<CrewUser> crewUsers = crewUserRepository.findByCrew(crew);// 그룹 내 속하는 멤버들

        fcmUtil.multiFcmSend(
                crewUsers.stream()
                        .map(CrewUser::getUser)
                        .toList(),
                fcmDTO
        );
    }

    }
