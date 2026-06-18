package auth

import (
	"context"
	"fmt"

	"google.golang.org/api/idtoken"
)

// VerifyGoogleIDToken validates the given Google ID token against the clientID
// and returns the subject (Google user ID) and email.
func VerifyGoogleIDToken(ctx context.Context, idTokenStr, clientID string) (sub, email string, err error) {
	payload, err := idtoken.Validate(ctx, idTokenStr, clientID)
	if err != nil {
		return "", "", fmt.Errorf("idtoken validate: %w", err)
	}
	sub = payload.Subject
	if sub == "" {
		return "", "", fmt.Errorf("empty subject in token payload")
	}
	emailVal, _ := payload.Claims["email"].(string)
	return sub, emailVal, nil
}
