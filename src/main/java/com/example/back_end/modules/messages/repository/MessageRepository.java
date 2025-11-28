package com.example.back_end.modules.messages.repository;

import com.example.back_end.modules.messages.entity.Message;
import com.example.back_end.modules.messages.entity.Message.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // الرسائل المستلمة
    Page<Message> findByToUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    // الرسائل المرسلة
    Page<Message> findByFromUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    // الرسائل غير المقروءة
    Integer countByToUserIdAndStatus(Integer userId, MessageStatus status);

    // الردود على رسالة معينة
    List<Message> findByParentMessageIdOrderByCreatedAtAsc(Long parentMessageId);

    // عدد الردود على رسالة
    Integer countByParentMessageId(Long parentMessageId);

    // البحث في الرسائل
    @Query("SELECT m FROM Message m WHERE " +
            "(m.toUser.id = :userId OR m.fromUser.id = :userId) AND " +
            "(LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.body) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Message> searchMessages(@Param("userId") Integer userId,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);

    // جلب رسالة مع التحقق من الصلاحية
    @Query("SELECT m FROM Message m WHERE m.id = :messageId AND " +
            "(m.toUser.id = :userId OR m.fromUser.id = :userId)")
    Optional<Message> findByIdAndUserAccess(@Param("messageId") Long messageId,
                                            @Param("userId") Integer userId);
}