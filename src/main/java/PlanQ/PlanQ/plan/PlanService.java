package PlanQ.PlanQ.plan;

import PlanQ.PlanQ.Member.Member;
import PlanQ.PlanQ.Member.MemberService;
import PlanQ.PlanQ.plan.dto.request.RequestPlanDto;
import PlanQ.PlanQ.plan.dto.response.ResponsePlanDto;
import PlanQ.PlanQ.report.dto.request.RequestReportDto;
import PlanQ.PlanQ.security.Dto.SecurityUserDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {
    private final PlanRepository planRepository;
    private final MemberService memberService;

    public Plan findById(Long id){
        return planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 Plan Entity 찾지 못함: " + id));
    }
    public List<ResponsePlanDto> viewAllByMemberAndYearMonth(Long yearMonth){
        Member member = memberService.getMember();
        Long year = yearMonth/100;
        Long month = yearMonth%100;
        return planRepository.findAllByMemberAndYearMonth(member, year, month).stream()
                .map(Plan :: toResponsePlanDto)
                .toList();
    }

    @Transactional
    public boolean createPlan(RequestPlanDto requestPlanDto){
        Member member = memberService.getMember();
        Plan plan = requestPlanDto.toEntity(member);
        planRepository.save(plan);
        return true;
    }

    @Transactional
    public boolean editPlan(Long planId, RequestPlanDto requestPlanDto){
        Member member = memberService.getMember();
        Plan plan = findById(planId);
        if(!member.equals(plan.getMember())){
            log.error("수정 불가");
            return false;
        }
        plan.update(requestPlanDto);
        return true;
    }

    @Transactional
    public boolean deletePlan(Long planId){
        Member member = memberService.getMember();
        Plan plan = findById(planId);
        if(!member.equals(plan.getMember())){
            log.error("삭제 불가");
            return false;
        }
        planRepository.delete(plan);
        return true;
    }

    @Transactional
    public boolean clearPlan(Long planId) {
        Member member = memberService.getMember();
        Plan plan = findById(planId);
        if(!member.equals(plan.getMember())){
            log.error("클리어 불가");
            return false;
        }
        plan.clear();
        return true;
    }

}
