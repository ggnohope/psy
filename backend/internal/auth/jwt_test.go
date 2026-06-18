package auth

import (
	"testing"
)

func TestJWTRoundTrip(t *testing.T) {
	const secret = "test-secret"
	const wantUserID = int64(42)

	tokenStr, err := IssueJWT(wantUserID, secret)
	if err != nil {
		t.Fatalf("IssueJWT: %v", err)
	}
	if tokenStr == "" {
		t.Fatal("IssueJWT returned empty token")
	}

	gotUserID, err := ParseJWT(tokenStr, secret)
	if err != nil {
		t.Fatalf("ParseJWT: %v", err)
	}
	if gotUserID != wantUserID {
		t.Errorf("userID = %d, want %d", gotUserID, wantUserID)
	}
}

func TestParseJWT_GarbageToken(t *testing.T) {
	_, err := ParseJWT("this.is.garbage", "any-secret")
	if err == nil {
		t.Error("expected error for garbage token, got nil")
	}
}

func TestParseJWT_WrongSecret(t *testing.T) {
	tokenStr, err := IssueJWT(99, "secret-a")
	if err != nil {
		t.Fatalf("IssueJWT: %v", err)
	}
	_, err = ParseJWT(tokenStr, "secret-b")
	if err == nil {
		t.Error("expected error when verifying with wrong secret, got nil")
	}
}
