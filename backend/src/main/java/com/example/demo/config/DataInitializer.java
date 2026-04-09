package com.example.demo.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.model.Comment;
import com.example.demo.model.Friendships;
import com.example.demo.model.MessageBoard;
import com.example.demo.model.Roles;
import com.example.demo.model.User;
import com.example.demo.repository.FriendshipsRepository;
import com.example.demo.repository.MessageBoardRepository;
import com.example.demo.repository.RolesRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageBoardRepository messageBoardRepository;
    private final FriendshipsRepository friendshipsRepository;

    @Override
    public void run(String... args) throws Exception {
        // 初始化角色 (Admin & User)
        if (rolesRepository.findByRoleName("ROLE_ADMIN").isEmpty()) {
            rolesRepository.save(new Roles("ROLE_ADMIN"));
        }

        Roles userRole = rolesRepository.findByRoleName("ROLE_USER")
                .orElseGet(() -> rolesRepository.save(new Roles("ROLE_USER")));

        // 初始化管理員 (admin / 1111) 
        if (userRepository.findByEmail("admin@test.com").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@test.com");
            admin.setPasswordHash(passwordEncoder.encode("1111"));
            
            rolesRepository.findByRoleName("ROLE_ADMIN").ifPresent(admin::setRole);
            admin.setIsActive(true);
            admin.setProfilePictureUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=admin");
            
            userRepository.save(admin);
            System.out.println("已建立管理員帳號: admin / 1111");
        }

        // 產生 10 個假帳號 (台灣人名)
        List<User> createdUsers = new ArrayList<>();
        String[] names = {"陳小明", "林怡君", "張雅婷", "王志豪", "李淑芬", "楊宗緯", "黃冠宇", "吳家豪", "蔡欣怡", "許家瑋"};
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            
            if (!userRepository.existsByUsername(name)) {
                User user = new User();
                user.setUsername(name);
                user.setPasswordHash(passwordEncoder.encode("1111")); // 密碼統一為 1111
                user.setEmail("user" + (i + 1) + "@test.com");
                user.setRole(userRole);
                user.setIsActive(true);
                user.setPoints(10000); // 預設給點數方便測試
                user.setProfilePictureUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + name);
                
                createdUsers.add(userRepository.save(user));
                System.out.println("已自動建立測試帳號: " + name);
            } else {
                userRepository.findByUsername(name).ifPresent(createdUsers::add);
            }
        }

        // 建立一個展示帳號 (名稱/信箱/密碼 皆為 1)
        User demoUser;
        if (!userRepository.existsByUsername("1")) {
            User newDemoUser = new User();
            newDemoUser.setUsername("1");
            newDemoUser.setEmail("1");
            newDemoUser.setPasswordHash(passwordEncoder.encode("1"));
            newDemoUser.setRole(userRole);
            newDemoUser.setIsActive(true);
            newDemoUser.setPoints(50000);
            newDemoUser.setProfilePictureUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=demo1");
            demoUser = userRepository.save(newDemoUser);
            System.out.println("已建立展示帳號: 1 / 1");
        } else {
            demoUser = userRepository.findByUsername("1").get();
        }

        // 建立好友關係：讓展示帳號 '1' 與其中 4 個帳號成為好友
        if (createdUsers.size() >= 4) {
            for (int i = 0; i < 4; i++) {
                User friend = createdUsers.get(i);
                if (!friendshipsRepository.existsByRequesterAndReceiver(demoUser, friend) &&
                    !friendshipsRepository.existsByRequesterAndReceiver(friend, demoUser)) {
                    Friendships friendship = new Friendships();
                    friendship.setRequester(demoUser);
                    friendship.setReceiver(friend);
                    friendship.setStatus(1); // 1 = Accepted
                    friendshipsRepository.save(friendship);
                }
            }
        }
        System.out.println("已為展示帳號 '1' 建立 4 個好友關係");

        // 4. 產生 500 篇假文章 (若資料庫文章少於 100 篇)
        if (messageBoardRepository.count() < 100) {
            List<User> users = userRepository.findAll();
            if (!users.isEmpty()) {
                Random random = new Random();
                List<MessageBoard> posts = new ArrayList<>();
                
                String[] contents = {
                    "今天天氣真好，適合出去走走！",
                    "最近發現一家超好吃的餐廳，推薦給大家。",
                    "有人在看最近很紅的那部劇嗎？結局太意外了！",
                    "工作好累，想放假去旅行...",
                    "分享一張隨手拍的風景照。",
                    "早安！新的一天開始了，加油！",
                    "剛學會了一個新技能，超有成就感。",
                    "有沒有人推薦好用的藍芽耳機？",
                    "心情有點低落，求安慰...",
                    "這週末有什麼活動推薦嗎？",
                    "剛看完一本書，感觸良多。",
                    "好久沒運動了，全身痠痛。",
                    "今天的咖啡特別香。",
                    "下雨天最適合在家睡覺。",
                    "有人知道這首歌的歌名嗎？",
                    "期待已久的包裹終於到了！",
                    "晚餐吃什麼好呢？選擇困難症發作。",
                    "路邊的小貓好可愛。",
                    "終於把專案做完了，開心！",
                    "好想去海邊吹吹風。",
                    "最近迷上了做甜點。",
                    "大家有推薦的 Netflix 影集嗎？",
                    "今天遇到一個很久沒見的朋友。",
                    "手機快沒電了，焦慮。",
                    "減肥計畫第一天，希望能堅持下去。",
                    "好想養一隻狗喔。",
                    "今天的夕陽好美。",
                    "有人要一起去打球嗎？",
                    "最近睡眠品質不太好，有什麼改善方法？",
                    "剛買的新衣服穿起來很合身。",
                    "好想吃火鍋啊！",
                    "今天發生了一件很幸運的事。",
                    "有人懂電腦嗎？求救！",
                    "準備開始學日文了。",
                    "週末要去露營，好期待。",
                    "家裡的植物終於開花了。",
                    "好想喝珍珠奶茶。",
                    "最近工作壓力好大。",
                    "有人去過這家展覽嗎？值得去嗎？",
                    "今天也是努力奮鬥的一天。",
                    "好想換新髮型，有推薦的設計師嗎？",
                    "剛看完一場很棒的電影。",
                    "有人在玩這個遊戲嗎？求隊友。",
                    "天氣忽冷忽熱，大家要小心感冒。",
                    "好想去日本旅遊。",
                    "今天的便當好好吃。",
                    "有人知道哪裡有賣好吃的貝果嗎？",
                    "最近在練習冥想，感覺不錯。",
                    "好想放長假啊！",
                    "祝大家有個美好的一天！"
                };
                
                String[] commentContents = {
                    "真的嗎？", "太棒了！", "我也這麼覺得。", "哈哈哈哈", "感謝分享",
                    "下次去試試看", "這在哪裡？", "好羨慕喔", "加油！", "拍拍",
                    "完全同意", "笑死", "這是什麼？", "好厲害", "我也想去",
                    "筆記筆記", "推一個", "早安", "晚安", "真的假的",
                    "有推薦的嗎？", "看起來不錯", "想知道+1", "路過", "專業"
                };

                for (int i = 0; i < 500; i++) {
                    MessageBoard post = new MessageBoard();
                    User randomUser = users.get(random.nextInt(users.size()));
                    post.setUser(randomUser);
                    
                    String content = contents[random.nextInt(contents.length)] + " (測試貼文 #" + (i + 1) + ")";
                    post.setContent(content);
                    
                    // 隨機時間 (過去 30 天內)
                    LocalDateTime time = LocalDateTime.now().minusHours(random.nextInt(24 * 30));
                    post.setCreatedAt(time);
                    post.setUpdatedAt(time);
                    
                    // 隨機讚數
                    post.setLikesCount(random.nextInt(50));
                    
                    // 隨機生成 1~20 則留言
                    int commentCount = random.nextInt(20) + 1;
                    for (int j = 0; j < commentCount; j++) {
                        Comment comment = new Comment();
                        comment.setMessageBoard(post);
                        comment.setUser(users.get(random.nextInt(users.size())));
                        comment.setContent(commentContents[random.nextInt(commentContents.length)]);
                        comment.setCreatedAt(time.plusMinutes(random.nextInt(60 * 24)));
                        post.getComments().add(comment);
                    }
                    post.setCommentsCount(post.getComments().size());
                    
                    // 70% 機率有圖片
                    if (random.nextInt(10) < 7) {
                        post.setImageUrl("https://picsum.photos/seed/" + (i + 1) + "/600/400");
                    }
                    
                    posts.add(post);
                }
                messageBoardRepository.saveAll(posts);
                System.out.println("已自動建立 500 篇測試文章");
            }
        } else {
            System.out.println("資料庫已有 " + messageBoardRepository.count() + " 篇貼文，跳過假資料生成");
        }
    }
}