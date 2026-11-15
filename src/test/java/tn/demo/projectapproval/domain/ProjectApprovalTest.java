package tn.demo.projectapproval.domain;

import org.junit.jupiter.api.Test;
import tn.demo.common.domain.ApproverId;
import tn.demo.project.domain.ApproverSnapshot;
import tn.demo.project.domain.ProjectId;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;

class ProjectApprovalTest {
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();
    private ProjectApproval start() {
        return ProjectApproval.start(new ProjectId(UUID.randomUUID()), Set.of(
                new ApproverSnapshot(new ApproverId(first), "First", "first@example.com"),
                new ApproverSnapshot(new ApproverId(second), "Second", "second@example.com")), now);
    }

    @Test
    void requiresEveryApprovalAndRepeatedDecisionIsNoOp() {
        var one = start().decide(first, ApprovalDecision.APPROVE, now);
        assertTrue(one.isPending());
        assertFalse(one.isApproved());
        assertFalse(one.isRejected());
        assertSame(one, one.decide(first, ApprovalDecision.APPROVE, now));
        var all = one.decide(second, ApprovalDecision.APPROVE, now);
        assertTrue(all.isApproved());
        assertFalse(all.isPending());
        assertFalse(all.isRejected());
        assertSame(all, all.decide(first, ApprovalDecision.APPROVE, now));
        assertThrows(IllegalStateException.class, () -> all.decide(first, ApprovalDecision.REJECT, now));
    }

    @Test
    void rejectionImmediatelyEndsSaga() {
        var rejected = start().decide(first, ApprovalDecision.REJECT, now);
        assertTrue(rejected.isRejected());
        assertFalse(rejected.isPending());
        assertFalse(rejected.isApproved());
        assertSame(rejected, rejected.decide(first, ApprovalDecision.REJECT, now));
        assertThrows(IllegalStateException.class, () -> rejected.decide(first, ApprovalDecision.APPROVE, now));
        assertThrows(IllegalStateException.class, () -> rejected.decide(second, ApprovalDecision.APPROVE, now));
        assertThrows(IllegalStateException.class, () -> rejected.decide(second, ApprovalDecision.REJECT, now));
    }

    @Test
    void cannotChangeDecisionOrUseUnknownApprover() {
        var one = start().decide(first, ApprovalDecision.APPROVE, now);
        assertThrows(IllegalStateException.class, () -> one.decide(first, ApprovalDecision.REJECT, now));
        assertThrows(NoSuchElementException.class, () -> one.decide(UUID.randomUUID(), ApprovalDecision.APPROVE, now));
    }
}
