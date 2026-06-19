package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"

	"walkie-server/internal/group"
	"walkie-server/internal/livekit"
)

type CreateGroupReq struct {
	Name string `json:"name"`
}

type JoinReq struct {
	Code string `json:"code"`
}

type TokenResp struct {
	LivekitURL string `json:"livekitUrl"`
	Token      string `json:"token"`
	Room       string `json:"room"`
	GroupName  string `json:"groupName"`
}

func main() {
	apiKey := getEnv("LIVEKIT_API_KEY", "devkey")
	apiSecret := getEnv("LIVEKIT_API_SECRET", "secret")

	store := group.NewStore()

	r := chi.NewRouter()
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)

	// CORS for Android dev (emulator + physical devices)
	r.Use(cors.Handler(cors.Options{
		AllowedOrigins:   []string{"*"},
		AllowedMethods:   []string{"GET", "POST", "OPTIONS"},
		AllowedHeaders:   []string{"Accept", "Authorization", "Content-Type"},
		AllowCredentials: false,
		MaxAge:           300,
	}))

	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte(`{"status":"ok"}`))
	})

	// Create group (anonymous for MVP)
	r.Post("/groups", func(w http.ResponseWriter, r *http.Request) {
		var req CreateGroupReq
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Name == "" {
			http.Error(w, `{"error":"name required"}`, http.StatusBadRequest)
			return
		}
		g := store.Create(req.Name)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(g)
	})

	// Join by code
	r.Post("/groups/join", func(w http.ResponseWriter, r *http.Request) {
		var req JoinReq
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil || len(req.Code) != 6 {
			http.Error(w, `{"error":"valid 6-char code required"}`, http.StatusBadRequest)
			return
		}
		g := store.Join(req.Code)
		if g == nil {
			http.Error(w, `{"error":"group not found"}`, http.StatusNotFound)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(g)
	})

	// Get token for PTT room. nickname passed as query for MVP
	r.Post("/groups/{code}/token", func(w http.ResponseWriter, r *http.Request) {
		code := chi.URLParam(r, "code")
		nickname := r.URL.Query().Get("nickname")
		if nickname == "" {
			nickname = "Anonymous"
		}

		g := store.GetByCode(code)
		if g == nil {
			http.Error(w, `{"error":"group not found"}`, http.StatusNotFound)
			return
		}

		roomName := "group-" + code
		token, err := livekit.GetJoinToken(apiKey, apiSecret, roomName, nickname)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		resp := TokenResp{
			LivekitURL: "ws://10.0.2.2:7880", // For Android emulator. Change to your LAN IP for real device
			Token:      token,
			Room:       roomName,
			GroupName:  g.Name,
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(resp)
	})

	// List (for debug, all groups)
	r.Get("/groups", func(w http.ResponseWriter, r *http.Request) {
		// For MVP we don't have per-user groups, return empty or implement later
		w.Header().Set("Content-Type", "application/json")
		w.Write([]byte("[]"))
	})

	log.Println("WalkieTalkie server listening on :8080")
	log.Println("LiveKit keys:", apiKey, "(secret hidden)")
	log.Fatal(http.ListenAndServe(":8080", r))
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
