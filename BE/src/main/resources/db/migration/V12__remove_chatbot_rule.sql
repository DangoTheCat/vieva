-- Remove only the retired chatbot prompt; retain rules for other AI features.
DELETE FROM ai_rules WHERE rule_code = 'STUDENT_ASSISTANT';
