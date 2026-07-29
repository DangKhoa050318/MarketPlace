package com.training.marketplace.service;

import com.training.marketplace.dto.request.VoteRequest;
import com.training.marketplace.dto.response.VoteResponse;

public interface ContentVoteService {

    VoteResponse toggleVote(Long userId, VoteRequest request);
}
