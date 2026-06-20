package auth

import (
	"context"
	"fmt"

	"google.golang.org/api/idtoken"
)

// VerifyGoogleIDToken validates the given Google ID token against any of the accepted
// clientIDs (audiences) and returns the subject (Google user ID) and email. It succeeds
// as soon as one audience validates; if none do, it returns the last validation error.
func VerifyGoogleIDToken(ctx context.Context, idTokenStr string, clientIDs []string) (sub, email string, err error) {
	if len(clientIDs) == 0 {
		return "", "", fmt.Errorf("no google client ids configured")
	}
	var lastErr error
	for _, clientID := range clientIDs {
		payload, e := idtoken.Validate(ctx, idTokenStr, clientID)
		if e != nil {
			lastErr = e
			continue
		}
		sub = payload.Subject
		if sub == "" {
			return "", "", fmt.Errorf("empty subject in token payload")
		}
		emailVal, _ := payload.Claims["email"].(string)
		return sub, emailVal, nil
	}
	return "", "", fmt.Errorf("idtoken validate (all %d audiences failed): %w", len(clientIDs), lastErr)
}
