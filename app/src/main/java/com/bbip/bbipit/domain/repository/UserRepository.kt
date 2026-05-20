package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.Friend
import com.bbip.bbipit.domain.entity.User
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.collections.emptyList

/**
 * 프로필 관리 및 친구 관계 설정 명시 기반 유저 데이터 처리 추상화 도메인 계층 리포지토리 인터페이스
 */
interface UserRepository {

    // 수락 완료 친구 목록 상태 배열 앱 전역 실시간 구독 공유 목적 데이터 플로우
    val myFriends: StateFlow<List<Friend>>

    // 내 고유 UID 기준 원격 저장소 친구 데이터 변경 내역 관찰 시작 흐름 구동 함수
    fun startObservingFriends(myUid: String)

    // 원격지 기준 푸시 서비스 작동 필수 가용 푸시 FCM 토큰 식별 코드 인출 함수
    suspend fun getFcmToken(): String?

    // 변경 요청 닉네임, 상태 메시지, 사진 주소 및 토큰 정보 토대 내 프로필 명세 원격 갱신 함수
    suspend fun updateProfile(nickname: String? = null, status: String? = null, profileImageUrl: String? = null, fcmToken: String? = null): Result<String>

    // 특정 사용자 UID 수신 대상자 지정 기반 신규 친구 맺기 요청 발신 함수
    suspend fun sendFriendRequest(targetUid: String): Result<String>

    // 기존 소셜 관계망 포함 특정 대상자 친구 연결 상태 영구 삭제 철회 함수
    suspend fun deleteFriend(targetUid: String): Result<String>

    // 상호 동의 수락 관계 성립 친구 전체 프로필 내역 목록 단발성 조회 인출 함수
    suspend fun getMyAcceptedFriends(): Result<List<Friend>>

    // 요청 목록 가져오기
    suspend fun getPendingFriendRequests(): Result<List<User>>

    // 수명 주기 동기화 목적 현재 사용자 체류 채팅방 문맥 정보 토대 세션 생존 신호 발생 함수
    suspend fun updateHeartbeat(currentRoomId: String?): Result<Unit>

    // 소켓 및 데이터베이스 활성화 네트워크 실시간 접속 유무 플래그 상태 상향 업데이트 함수
    suspend fun updateOnlineStatus(isOnline: Boolean): Result<Boolean>

    // 인입 대기 타인 친구 추가 요청 명세 대상 수락 의사 확정 함수
    suspend fun acceptFriendRequest(targetUid: String): Result<Boolean>

    // 인입 친구 요청 내역 대상 거절 의사 반영 및 관계 테이블 명세 파기 삭제 함수
    suspend fun declineFriendRequest(targetUid: String): Result<Boolean>

    // 타인 고유 식별 코드 기반 서버 공간 공개 등록 프로필 명세 엔티티 조회 인출 함수
    suspend fun getUserProfile(targetUid: String): Result<User>

    // 내 고유 식별 계정 키값 파라미터 대입 기반 마이페이지 개인 세부 프로필 엔티티 직접 조회 인출 함수
    suspend fun getMyProfile(uid: String): Result<User>

    // 특정 친구 상세 프로필 정보 조회 및 쌍방 상호 수락 대기 상태 관계 등급 문자열 코드 쌍 획득 함수
    suspend fun getFriendProfileWithStatus(targetUid: String): Result<Pair<User, String>>

}