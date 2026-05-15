package com.scrumtools.service;

import com.scrumtools.entity.User;
import com.scrumtools.entity.UserDashboard;
import com.scrumtools.repository.UserDashboardRepository;
import com.scrumtools.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserDashboardRepository dashboardRepository;
    private final UserRepository userRepository;

    /** Kullanıcının mevcut dashboard düzenini getirir; yoksa boş layout döner. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLayout() {
        User user = currentUser();
        return dashboardRepository.findByUserId(user.getId())
                .map(UserDashboard::getLayout)
                .orElse(new ArrayList<>());
    }

    /** Widget düzenini kaydeder (upsert). */
    @Transactional
    public List<Map<String, Object>> saveLayout(List<Map<String, Object>> layout) {
        User user = currentUser();
        UserDashboard dashboard = dashboardRepository.findByUserId(user.getId())
                .orElseGet(() -> UserDashboard.builder().user(user).build());
        dashboard.setLayout(layout);
        return dashboardRepository.save(dashboard).getLayout();
    }

    /** Tek widget ekle/güncelle (id ile). */
    @Transactional
    public List<Map<String, Object>> upsertWidget(Map<String, Object> widget) {
        User user = currentUser();
        UserDashboard dashboard = dashboardRepository.findByUserId(user.getId())
                .orElseGet(() -> UserDashboard.builder().user(user).build());

        String widgetId = (String) widget.get("id");
        List<Map<String, Object>> layout = new ArrayList<>(
                dashboard.getLayout() != null ? dashboard.getLayout() : new ArrayList<>()
        );

        layout.removeIf(w -> widgetId.equals(w.get("id")));
        layout.add(widget);
        dashboard.setLayout(layout);
        return dashboardRepository.save(dashboard).getLayout();
    }

    /** Widget sil. */
    @Transactional
    public List<Map<String, Object>> removeWidget(String widgetId) {
        User user = currentUser();
        UserDashboard dashboard = dashboardRepository.findByUserId(user.getId())
                .orElseGet(() -> UserDashboard.builder().user(user).build());

        List<Map<String, Object>> layout = new ArrayList<>(
                dashboard.getLayout() != null ? dashboard.getLayout() : new ArrayList<>()
        );
        layout.removeIf(w -> widgetId.equals(w.get("id")));
        dashboard.setLayout(layout);
        return dashboardRepository.save(dashboard).getLayout();
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + email));
    }
}

