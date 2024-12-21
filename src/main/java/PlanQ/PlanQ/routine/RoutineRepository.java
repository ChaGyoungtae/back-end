package PlanQ.PlanQ.routine;

import PlanQ.PlanQ.Member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutineRepository extends JpaRepository<Routine, Long> {
    List<Routine> findAllByMember(Member member);
}
