package livekit

import (
	"time"

	lksdk "github.com/livekit/server-sdk-go/v2"
	"github.com/livekit/protocol/auth"
)

// GetJoinToken generates a LiveKit access token for a given room.
// For MVP we allow publish by default; the Android client will control
// microphone publishing for classic PTT behavior.
func GetJoinToken(apiKey, apiSecret, room, identity string) (string, error) {
	at := auth.NewAccessToken(apiKey, apiSecret)
	grant := &auth.VideoGrant{
		RoomJoin: true,
		Room:     room,
		// For small PTT groups we can also set CanPublish: true
		// (client decides when to actually publish audio track)
	}
	at.SetVideoGrant(grant).
		SetIdentity(identity).
		SetValidFor(24 * time.Hour)
	return at.ToJWT()
}
