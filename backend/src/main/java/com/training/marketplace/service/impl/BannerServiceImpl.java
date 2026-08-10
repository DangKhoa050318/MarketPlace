package com.training.marketplace.service.impl;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateBannerRequest;
import com.training.marketplace.dto.request.UpdateBannerRequest;
import com.training.marketplace.dto.response.BannerResponse;
import com.training.marketplace.entity.MerchandisingBanner;
import com.training.marketplace.enums.MerchandisingTargetType;
import com.training.marketplace.enums.PublishStatus;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ConflictException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.BannerMapper;
import com.training.marketplace.repository.MerchandisingBannerRepository;
import com.training.marketplace.repository.MerchandisingEventRepository;
import com.training.marketplace.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final MerchandisingBannerRepository bannerRepository;
    private final MerchandisingEventRepository eventRepository;
    private final BannerMapper bannerMapper;

    @Override
    @Transactional
    public BannerResponse create(CreateBannerRequest request) {
        validateWindow(request.startsAt(), request.endsAt());
        MerchandisingBanner banner = bannerMapper.toEntity(request); // status=DRAFT, displayOrder=0, active=true
        MerchandisingBanner saved = bannerRepository.save(banner);
        log.info("Banner created: id={}, position={}", saved.getId(), saved.getPosition());
        return bannerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BannerResponse update(Long id, UpdateBannerRequest request) {
        MerchandisingBanner banner = findOr404(id);
        validateWindow(request.startsAt(), request.endsAt());
        bannerMapper.updateEntity(request, banner);
        return bannerMapper.toResponse(bannerRepository.save(banner));
    }

    @Override
    @Transactional(readOnly = true)
    public BannerResponse getById(Long id) {
        return bannerMapper.toResponse(findOr404(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BannerResponse> list(PublishStatus status, String position, Pageable pageable) {
        return PageResponse.from(bannerRepository.search(status, position, pageable), bannerMapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MerchandisingBanner banner = findOr404(id);
        banner.setActive(false);
        bannerRepository.save(banner);
        log.info("Banner soft-deleted: id={}", id);
    }

    @Override
    @Transactional
    public BannerResponse restore(Long id) {
        MerchandisingBanner banner = findOr404(id);
        banner.setActive(true);
        log.info("Banner restored (active=true): id={}", id);
        return bannerMapper.toResponse(bannerRepository.save(banner));
    }

    @Override
    @Transactional
    public void hardDelete(Long id) {
        MerchandisingBanner banner = findOr404(id);
        if (eventRepository.existsByTargetTypeAndTargetId(MerchandisingTargetType.BANNER, id)) {
            throw new ConflictException(
                    "Banner has recorded impression/click history and can only be deactivated, not permanently deleted");
        }
        bannerRepository.delete(banner);
        log.info("Banner hard-deleted: id={}", id);
    }

    @Override
    @Transactional
    public BannerResponse publish(Long id) {
        MerchandisingBanner banner = findOr404(id);
        banner.setStatus(PublishStatus.PUBLISHED);
        return bannerMapper.toResponse(bannerRepository.save(banner));
    }

    @Override
    @Transactional
    public BannerResponse unpublish(Long id) {
        MerchandisingBanner banner = findOr404(id);
        banner.setStatus(PublishStatus.DRAFT);
        return bannerMapper.toResponse(bannerRepository.save(banner));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> listEffectiveByPosition(String position) {
        return bannerRepository.findEffectiveByPosition(position, LocalDateTime.now())
                .stream().map(bannerMapper::toResponse).toList();
    }

    // ---------------------------------------------------------------- helpers

    private MerchandisingBanner findOr404(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner", id));
    }

    private void validateWindow(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt != null && endsAt != null && endsAt.isBefore(startsAt)) {
            throw new BadRequestException("endsAt must not be before startsAt");
        }
    }
}
