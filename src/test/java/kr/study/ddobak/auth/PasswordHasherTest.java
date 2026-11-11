package kr.study.ddobak.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    final PasswordHasher hasher = new PasswordHasher(1000);

    @Test
    void hash_neverContainsThePassword_andUsesNewSaltEachTime() {
        String a = hasher.hash("abcd1234");
        String b = hasher.hash("abcd1234");
        assertThat(a).startsWith("pbkdf2$1000$").doesNotContain("abcd1234");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void matches_onlyTheRightPassword() {
        String stored = hasher.hash("abcd1234");
        assertThat(hasher.matches("abcd1234", stored)).isTrue();
        assertThat(hasher.matches("abcd1235", stored)).isFalse();
        assertThat(hasher.matches("abcd1234", null)).isFalse();
        assertThat(hasher.matches("abcd1234", "broken")).isFalse();
    }

    @Test
    void matches_oldHashAfterIterationsChange() {
        String old = new PasswordHasher(500).hash("abcd1234");
        assertThat(hasher.matches("abcd1234", old)).isTrue();
    }
}
