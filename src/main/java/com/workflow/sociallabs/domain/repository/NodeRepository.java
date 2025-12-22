package com.workflow.sociallabs.domain.repository;

import com.workflow.sociallabs.domain.entity.Node;
import com.workflow.sociallabs.domain.enums.NodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository для роботи з Node definitions
 */
@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {

    /**
     * Знайти ноду за типом
     */
    Optional<Node> findByType(NodeType type);

    /**
     * Пошук нод за назвою або описом
     */
    @Query("""
        SELECT n FROM Node n
        WHERE LOWER(n.name) LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    List<Node> searchNodes(@Param("search") String search);
}