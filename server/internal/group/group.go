package group

import (
	"crypto/rand"
	"encoding/hex"
	"sync"
	"time"
)

type Group struct {
	ID        string    `json:"id"`
	Code      string    `json:"code"`
	Name      string    `json:"name"`
	CreatedAt time.Time `json:"created_at"`
}

type Store struct {
	mu     sync.RWMutex
	byCode map[string]*Group
}

func NewStore() *Store {
	return &Store{
		byCode: make(map[string]*Group),
	}
}

func (s *Store) Create(name string) *Group {
	s.mu.Lock()
	defer s.mu.Unlock()

	code := generateCode()
	for s.byCode[code] != nil {
		code = generateCode()
	}

	g := &Group{
		ID:        generateID(),
		Code:      code,
		Name:      name,
		CreatedAt: time.Now(),
	}
	s.byCode[code] = g
	return g
}

func (s *Store) GetByCode(code string) *Group {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.byCode[code]
}

func (s *Store) Join(code string) *Group {
	return s.GetByCode(code)
}

func generateCode() string {
	b := make([]byte, 3)
	rand.Read(b)
	// 6 char: uppercase letters + digits
	hexStr := hex.EncodeToString(b)
	// simple map to alphanum
	code := ""
	for i := 0; i < len(hexStr) && len(code) < 6; i++ {
		c := hexStr[i]
		if c >= '0' && c <= '9' {
			code += string(c)
		} else if c >= 'a' && c <= 'f' {
			code += string('A' + (c - 'a'))
		}
	}
	if len(code) < 6 {
		code += "ABCDEF"[:6-len(code)]
	}
	return code[:6]
}

func generateID() string {
	b := make([]byte, 8)
	rand.Read(b)
	return hex.EncodeToString(b)
}
