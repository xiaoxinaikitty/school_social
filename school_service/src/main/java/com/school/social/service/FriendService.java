package com.school.social.service;

import com.school.social.common.PageResponse;
import com.school.social.dto.friend.FriendCandidateView;
import com.school.social.dto.friend.FriendRequestCreateRequest;
import com.school.social.dto.friend.FriendRequestRespondRequest;

public interface FriendService {
    PageResponse<FriendCandidateView> searchCandidates(Long userId, String keyword, int page, int size);

    void sendRequest(Long userId, FriendRequestCreateRequest request);

    void respondRequest(Long userId, Long requestId, FriendRequestRespondRequest request);
}
