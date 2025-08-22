-- Create transcript_status table for tracking AssemblyAI transcript processing
CREATE TABLE IF NOT EXISTS transcript_status (
    id UUID PRIMARY KEY,
    transcript_id UUID UNIQUE NOT NULL,
    task_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    transcript_text TEXT,
    confidence DOUBLE PRECISION,
    audio_duration DOUBLE PRECISION,
    error_message TEXT,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_transcript_status_transcript_id ON transcript_status(transcript_id);
CREATE INDEX IF NOT EXISTS idx_transcript_status_task_id ON transcript_status(task_id);
CREATE INDEX IF NOT EXISTS idx_transcript_status_status ON transcript_status(status);
CREATE INDEX IF NOT EXISTS idx_transcript_status_created_by ON transcript_status(created_by);
CREATE INDEX IF NOT EXISTS idx_transcript_status_created_at ON transcript_status(created_at);

-- Add comments for documentation
COMMENT ON TABLE transcript_status IS 'Tracks the status of AssemblyAI transcript processing requests';
COMMENT ON COLUMN transcript_status.transcript_id IS 'UUID returned by AssemblyAI when creating a transcript request';
COMMENT ON COLUMN transcript_status.task_id IS 'Reference to the listening task that owns this transcript';
COMMENT ON COLUMN transcript_status.status IS 'Current status: queued, processing, completed, error';
COMMENT ON COLUMN transcript_status.transcript_text IS 'The final transcript text when completed';
COMMENT ON COLUMN transcript_status.confidence IS 'Confidence score from AssemblyAI (0.0 to 1.0)';
COMMENT ON COLUMN transcript_status.audio_duration IS 'Duration of the audio file in seconds';
COMMENT ON COLUMN transcript_status.error_message IS 'Error details if status is error';
