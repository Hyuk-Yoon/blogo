package me.shinsunyoung.springbootdeveloper.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import me.shinsunyoung.springbootdeveloper.domain.Article;
import me.shinsunyoung.springbootdeveloper.domain.User;
import me.shinsunyoung.springbootdeveloper.dto.AddArticleRequest;
import me.shinsunyoung.springbootdeveloper.dto.ArticleResponse;
import me.shinsunyoung.springbootdeveloper.dto.UpdateArticleRequest;
import me.shinsunyoung.springbootdeveloper.repository.BlogRepository;
import me.shinsunyoung.springbootdeveloper.repository.UserRepository;
import me.shinsunyoung.springbootdeveloper.service.BlogService;
import org.aspectj.lang.annotation.Before;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc // MocMvc 생성 및 자동 구성
class BlogApiControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper; // 직렬화, 역직렬화

    @Autowired
    private WebApplicationContext context;

    @Autowired
    BlogRepository blogRepository;

    @Autowired
    BlogService blogService;

    @Autowired
    UserRepository userRepository;

    User user;

    @BeforeEach // 테스트 실행 전 실행하는 메서드
    @Transactional
    public void mockMvcSetUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        blogRepository.deleteAll();

        //List<Article> articles = blogRepository.findAll();
        //System.out.println("BeforeEach! size : " + articles.size());

    }

    @BeforeEach
    void setSecurityContext() {
        userRepository.deleteAll();
        user = userRepository.save(User.builder()
                .email("user@gmail.com")
                .password("test")
                .build());

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));
    }

    @DisplayName("addArticle: 블로그 글 추가에 성공한다.")
    @Test
    public void addArticle() throws Exception {
        // given
        final String url = "/api/articles";
        final String title = "title";
        final String content = "content";
        final AddArticleRequest userRequest = new AddArticleRequest(title, content);

        final String requestBody = objectMapper.writeValueAsString(userRequest);

        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn("username");

        // when
        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .principal(principal)
                .content(requestBody));

        // then
        result.andExpect(status().isCreated());

        List<Article> articles = blogRepository.findAll();

        assertThat(articles.size()).isEqualTo(1);
        assertThat(articles.get(0).getTitle()).isEqualTo(title);
        assertThat(articles.get(0).getContent()).isEqualTo(content);
    }



    @DisplayName("findAllArticles: 블로그 글 목록 조회에 성공한다.")
    @Test
    public void findAllArticles() throws Exception {
        // given : 블로그 글을 저장한다.
        final String url = "/api/articles";
        Article savedArticle = createDefaultArticle();

        // when : 목록 조회 API를 호출한다.
        ResultActions result = mockMvc.perform(get(url)
                .contentType(MediaType.APPLICATION_JSON)
        );

        // then : 응답 코드가 200 OK, 0번째 요소의 title,content 같은지 확인
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value(savedArticle.getTitle()))
                .andExpect(jsonPath("$[0].content").value(savedArticle.getContent()));

    }

    @DisplayName("findArticle: 블로그 글 조회에 성공한다.")
    @Test
    public void findArticle() throws Exception {
        // given : 블로그 글을 저장한다.
        final String url = "/api/articles/{id}";

        Article savedArticle = createDefaultArticle();

        // when : 글 조회 API를 호출한다.
        ResultActions result = mockMvc.perform(get(url, savedArticle.getId()));


        // then : 응답 코드가 200 OK, 0번째 요소의 title,content 같은지 확인
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(savedArticle.getTitle()))
                .andExpect(jsonPath("$.content").value(savedArticle.getContent()));

    }

    @DisplayName("deleteArticle: 블로그 글 삭제에 성공한다.")
    @Test
    public void deleteArticle() throws Exception {
        // given : 블로그 글을 저장한다.
        final String url = "/api/articles/{id}";
        Article savedArticle = createDefaultArticle();

        // when : 글 조회 API를 호출한다.
        ResultActions result = mockMvc.perform(delete(url, savedArticle.getId()));


        // then : 응답 코드가 200 OK, 전체를 조회해 배열 크기가 0인지 확인
        List<Article> articles = blogRepository.findAll();

        result.andExpect(status().isOk());

        assertThat(articles).isEmpty();
        assertThat(articles.size()).isEqualTo(0);
    }

    @DisplayName("updateArticle: 블로그 글 수정에 성공한다.")
    @Test
    public void updateArticle() throws Exception {
        // given : 블로그 글을 저장한다. 수정에 필요한 요청 객체를 만든다.
        final String url = "/api/articles/{id}";
        final String newTitle = "새 글";
        final String newContent = "새 본문";

        Article savedArticle = createDefaultArticle();

        UpdateArticleRequest request = new UpdateArticleRequest(newTitle, newContent);
        String serializedRequest = objectMapper.writeValueAsString(request);

        // when : 글 조회 API를 호출한다.
        ResultActions result = mockMvc.perform(put(url, savedArticle.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedRequest));


        // then : 응답 코드가 200 OK, 블로그 글 id 조회후에 값 수정 확인
        result
                .andExpect(status().isOk());

        Article article = blogRepository.findById(savedArticle.getId()).get();
        //Article article = blogService.findById(savedArticle.getId());

        assertThat(article.getTitle()).isEqualTo(newTitle);
        assertThat(article.getContent()).isEqualTo(newContent);

    }

    private Article createDefaultArticle() {

        return blogRepository.save(Article.builder()
                .title("title")
                .content("content")
                .author(user.getUsername())
                .build());
    }
}