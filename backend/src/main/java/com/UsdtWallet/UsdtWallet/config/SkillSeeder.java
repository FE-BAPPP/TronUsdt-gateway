package com.UsdtWallet.UsdtWallet.config;

import com.UsdtWallet.UsdtWallet.model.entity.Skill;
import com.UsdtWallet.UsdtWallet.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 🌱 SKILL DATA SEEDER
 * 
 * Tự động seed skills vào database khi app khởi động (nếu chưa có)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SkillSeeder implements CommandLineRunner {

    private final SkillRepository skillRepository;

    @Override
    public void run(String... args) {
        // Only seed if database is empty
        if (skillRepository.count() > 0) {
            log.info("✅ Skills already exist in database ({}), skipping seed", skillRepository.count());
            return;
        }

        log.info("🌱 Seeding skills into database...");

        List<String> defaultSkills = Arrays.asList(
            // Programming Languages
            "JavaScript", "TypeScript", "Python", "Java", "C#", "PHP", "Ruby", "Go", "Rust", "Swift", "Kotlin",
            
            // Frontend
            "React", "Vue.js", "Angular", "HTML", "CSS", "Sass", "Tailwind CSS", "Bootstrap",
            "Next.js", "Nuxt.js", "Svelte", "jQuery",
            
            // Backend
            "Node.js", "Express.js", "Django", "Flask", "Spring Boot", "ASP.NET", ".NET Core",
            "Laravel", "Ruby on Rails", "FastAPI",
            
            // Mobile
            "React Native", "Flutter", "iOS Development", "Android Development", "Ionic",
            
            // Database
            "MySQL", "PostgreSQL", "MongoDB", "Redis", "Firebase", "SQL Server", "Oracle",
            "DynamoDB", "Cassandra", "Elasticsearch",
            
            // Cloud & DevOps
            "AWS", "Azure", "Google Cloud", "Docker", "Kubernetes", "Jenkins", "CI/CD",
            "Terraform", "Ansible", "Linux", "Nginx",
            
            // Other
            "GraphQL", "REST API", "Microservices", "Git", "Testing", "Agile", "Scrum",
            "UI/UX Design", "Figma", "Adobe XD", "Photoshop", "Illustrator",
            
            // Blockchain (for your USDT platform)
            "Blockchain", "Solidity", "Web3", "Smart Contracts", "Ethereum", "TRON",
            "Cryptocurrency", "DeFi"
        );

        int count = 0;
        for (String skillName : defaultSkills) {
            try {
                Skill skill = Skill.builder()
                    .name(skillName)
                    .build();
                skillRepository.save(skill);
                count++;
            } catch (Exception e) {
                log.warn("Failed to seed skill: {} - {}", skillName, e.getMessage());
            }
        }

        log.info("✅ Successfully seeded {} skills into database", count);
    }
}
