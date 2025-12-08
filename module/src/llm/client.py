import json
import logging

import requests

from src.config import get_settings

logger = logging.getLogger(__name__)


class OllamaClient:
    def __init__(self):
        self.settings = get_settings()
        self.base_url = self.settings.ollama.base_url
        self.model = self.settings.ollama.model

    def extract_json(self, content: str, prompt: str) -> dict | None:
        system_prompt = """You are a data extraction assistant.
Extract data from the given content according to the user's prompt.
Always respond with valid JSON only, no additional text or explanation."""

        user_prompt = f"""Content to extract from:
```
{content}
```

Extraction prompt: {prompt}

Respond with JSON only."""

        try:
            response = requests.post(
                f"{self.base_url}/api/generate",
                json={
                    "model": self.model,
                    "prompt": user_prompt,
                    "system": system_prompt,
                    "stream": False,
                    "format": "json",
                },
                timeout=self.settings.ollama.timeout_sec,
            )
            response.raise_for_status()

            result = response.json()
            response_text = result.get("response", "")

            return json.loads(response_text)
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse LLM response as JSON: {e}")
            return None
        except requests.RequestException as e:
            logger.error(f"Ollama request failed: {e}")
            raise
