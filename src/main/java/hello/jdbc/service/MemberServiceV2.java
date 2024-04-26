package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 트랜잭션 - 파라미터 연동, 풀을 고려한 종료
 */
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV2 {

    private final MemberRepositoryV2 memberRepository;
    private final DataSource dataSource;

    public void accountTransfer(String fromId, String toId, int amount) throws SQLException {
        Connection conn = dataSource.getConnection();
        try {
            conn.setAutoCommit(false);
            // 비지니스 로직
            bizLogic(conn, fromId, toId, amount);

            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw new IllegalStateException(e);
        } finally {
            release(conn);
        }

    }

    private void bizLogic(Connection conn, String fromId, String toId, int amount) throws SQLException {
        Member fromMember = memberRepository.findById(conn, fromId);
        Member toMember = memberRepository.findById(conn, toId);

        memberRepository.update(conn, fromId, fromMember.getMoney() - amount);
        validation(toMember);
        memberRepository.update(conn, toId, toMember.getMoney() + amount);
    }

    private static void release(Connection conn) {
        if(conn != null) {
            try {
                // 커넥션풀로 돌아가기전에 AutoCommitMode 를 true 로 바꿔준다
                conn.setAutoCommit(true);
                conn.close();
            } catch (Exception e) {
                log.error("error", e);
            }
        }
    }

    private void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체중 예외 발생");
        }
    }


}
