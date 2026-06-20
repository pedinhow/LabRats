package br.com.starter.application.api.audio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.starter.domain.audio.AudioTranscriptionService;

class AudioTranscriptionControllerTest {

    @Test
    void receivesMultipartAudioAndReturnsTranscribedText() throws Exception {
        MockMvc mockMvc = createMockMvc();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "question.mp3",
                "audio/mpeg",
                new byte[] { 'I', 'D', '3', 1, 2, 3 }
        );

        mockMvc.perform(multipart("/audio/transcribe").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Texto transcrito."));
    }

    @Test
    void rejectsMissingFile() throws Exception {
        MockMvc mockMvc = createMockMvc();

        mockMvc.perform(multipart("/audio/transcribe"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsEmptyFile() throws Exception {
        MockMvc mockMvc = createMockMvc();
        MockMultipartFile file = new MockMultipartFile("file", "empty.mp3", "audio/mpeg", new byte[0]);

        mockMvc.perform(multipart("/audio/transcribe").file(file))
                .andExpect(status().isBadRequest());
    }

    private MockMvc createMockMvc() {
        AudioTranscriptionService service = new AudioTranscriptionService((filename, contentType, audio) ->
                "Texto transcrito."
        );
        return MockMvcBuilders
                .standaloneSetup(new AudioTranscriptionController(service))
                .build();
    }
}
